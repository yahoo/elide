/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.executors;

import static com.yahoo.elide.core.security.permissions.ExpressionResult.DEFERRED;
import static com.yahoo.elide.core.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.core.security.permissions.ExpressionResult.PASS;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.NonTransferable;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.security.permissions.ExpressionResultCache;
import com.yahoo.elide.core.security.permissions.PermissionExpressionBuilder;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.type.Type;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Triple;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Default permission executor.
 * This executor executes all security checks as outlined in the documentation.
 */
@Slf4j
public class ActivePermissionExecutor implements PermissionExecutor {
    private final Queue<QueuedCheck> commitCheckQueue = new LinkedBlockingQueue<>();

    private final RequestScope requestScope;
    private final PermissionExpressionBuilder expressionBuilder;
    private final Map<Triple<Class<? extends Annotation>, Type, ImmutableSet<String>>, ExpressionResult>
            userPermissionCheckCache;
    private final Map<String, Long> checkStats;
    private final boolean verbose;

    /**
     * Constructor.
     *
     * @param requestScope Request scope
     */
    public ActivePermissionExecutor(final RequestScope requestScope) {
        this(false, requestScope);
    }

    /**
     * Constructor.
     *
     * @param verbose True if executor should produce verbose output to caller
     * @param requestScope Request scope
     */
    public ActivePermissionExecutor(boolean verbose, final RequestScope requestScope) {
        ExpressionResultCache cache = new ExpressionResultCache();

        this.requestScope = requestScope;
        this.expressionBuilder = new PermissionExpressionBuilder(cache, requestScope.getDictionary());
        userPermissionCheckCache = new HashMap<>();
        checkStats = new HashMap<>();
        this.verbose = verbose;
    }

    @Override
    public <A extends Annotation> ExpressionResult checkPermission(
            Class<A> annotationClass,
            PersistentResource resource,
            Set<String> requestedFields

    ) {
        Supplier<Expression> expressionSupplier = () -> {
            if (NonTransferable.class == annotationClass) {
                if (requestScope.getDictionary().isTransferable(resource.getResourceType())) {
                    return expressionBuilder.buildAnyFieldExpressions(resource, ReadPermission.class,
                            requestedFields, null);
                }
                return PermissionExpressionBuilder.FAIL_EXPRESSION;
            }
            return expressionBuilder.buildAnyFieldExpressions(resource, annotationClass, requestedFields, null);
        };

        Function<Expression, ExpressionResult> expressionExecutor = (expression) -> {
            // for newly created object in PatchRequest limit to User checks
            if (resource.isNewlyCreated()) {
                return executeUserChecksDeferInline(annotationClass, expression);
            }
            return executeExpressions(expression, annotationClass, Expression.EvaluationMode.INLINE_CHECKS_ONLY);
        };

        return checkPermissions(
                resource.getResourceType(),
                annotationClass,
                requestedFields,
                expressionSupplier,
                expressionExecutor);
    }

    /**
     * We will only run User checks during patch extension for newly created objects because relationships are not yet
     * complete. The inline checks may fail (relationships are not yet fixed up). If the user checks return deferred, we
     * will defer all of the inline checks to commit phase.
     */
    private <A extends Annotation> ExpressionResult executeUserChecksDeferInline(Class<A> annotationClass,
            Expression expression) {
        ExpressionResult result =
                executeExpressions(expression, annotationClass, Expression.EvaluationMode.USER_CHECKS_ONLY);
        if (result == DEFERRED) {
            commitCheckQueue.add(new QueuedCheck(expression, annotationClass));
        }
        return result;
    }

    /**
     * Check for permissions on a specific field.
     *
     * @param <A> type parameter
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     */
    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissions(PersistentResource<?> resource,
                                                                                 ChangeSpec changeSpec,
                                                                                 Class<A> annotationClass,
                                                                                 String field) {
        Supplier<Expression> expressionSupplier = () ->
            expressionBuilder.buildSpecificFieldExpressions(resource, annotationClass, field, changeSpec);

        Function<Expression, ExpressionResult> expressionExecutor = expression ->
            executeExpressions(expression, annotationClass, Expression.EvaluationMode.INLINE_CHECKS_ONLY);

        return checkPermissions(
                resource.getResourceType(),
                annotationClass,
                Collections.singleton(field),
                expressionSupplier,
                expressionExecutor);
    }

    /**
     * Check for permissions on a specific field deferring all checks.
     *
     * @param <A> type parameter
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     */
    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                                         ChangeSpec changeSpec,
                                                                                         Class<A> annotationClass,
                                                                                         String field) {
        //We would want to evaluate the expression in the CreatePermission in case of
        // update checks on newly created entities
        Class expressionAnnotation = annotationClass.isAssignableFrom(UpdatePermission.class)
                && requestScope.getNewResources().contains(resource)
                ? CreatePermission.class
                : annotationClass;

        Supplier<Expression> expressionSupplier = () ->
                expressionBuilder.buildSpecificFieldExpressions(
                        resource,
                        expressionAnnotation,
                        field,
                        changeSpec);

        Function<Expression, ExpressionResult> expressionExecutor = (expression) -> {
            if (requestScope.getNewPersistentResources().contains(resource)) {
                return executeUserChecksDeferInline(expressionAnnotation, expression);
            }
            return executeExpressions(expression, expressionAnnotation, Expression.EvaluationMode.INLINE_CHECKS_ONLY);
        };

        return checkPermissions(
                resource.getResourceType(),
                expressionAnnotation,
                Collections.singleton(field),
                expressionSupplier,
                expressionExecutor);
    }

    /**
     * Check strictly user permissions on an entity.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     */
    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        Set<String> requestedFields) {
        Supplier<Expression> expressionSupplier = () ->
            expressionBuilder.buildUserCheckAnyExpression(
                    resourceClass,
                    annotationClass,
                    requestedFields,
                    requestScope);

        return checkOnlyUserPermissions(
                resourceClass,
                annotationClass,
                requestedFields,
                expressionSupplier);
    }

    /**
     * First attempts to check user permissions (by looking in the cache and if not present by executing user
     * permissions).  If user permissions don't short circuit the check, run the provided expression executor.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param fields Set of all field names that is being accessed
     * @param expressionSupplier Builds a permission expression.
     * @param expressionExecutor Evaluates the expression (post user check evaluation)
     */
    protected <A extends Annotation> ExpressionResult checkPermissions(
            Type<?> resourceClass,
            Class<A> annotationClass,
            Set<String> fields,
            Supplier<Expression> expressionSupplier,
            Optional<Function<Expression, ExpressionResult>> expressionExecutor) {

        // If the user check has already been evaluated before, return the result directly and save the building cost
        ImmutableSet<String> immutableFields = fields == null ? null : ImmutableSet.copyOf(fields);
        ExpressionResult expressionResult
                = userPermissionCheckCache.get(Triple.of(annotationClass, resourceClass, immutableFields));

        if (expressionResult == PASS) {
            return expressionResult;
        }

        Expression expression = expressionSupplier.get();

        if (expressionResult == null) {
            expressionResult = executeExpressions(
                    expression,
                    annotationClass,
                    Expression.EvaluationMode.USER_CHECKS_ONLY);

            userPermissionCheckCache.put(
                    Triple.of(annotationClass, resourceClass, immutableFields), expressionResult);

            if (expressionResult == PASS) {
                return expressionResult;
            }
        }

        return expressionExecutor
                .map(executor -> executor.apply(expression))
                .orElse(expressionResult);
    }

    /**
     * Only executes user permissions.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param fields Set of all field names that is being accessed
     * @param expressionSupplier Builds a permission expression.
     */
    protected <A extends Annotation> ExpressionResult checkOnlyUserPermissions(
            Type<?> resourceClass,
            Class<A> annotationClass,
            Set<String> fields,
            Supplier<Expression> expressionSupplier) {

        return checkPermissions(
                resourceClass,
                annotationClass,
                fields,
                expressionSupplier,
                Optional.empty()
        );
    }

    /**
     * First attempts to check user permissions (by looking in the cache and if not present by executing user
     * permissions).  If user permissions don't short circuit the check, run the provided expression executor.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param fields Set of all field names that is being accessed
     * @param expressionSupplier Builds a permission expression.
     * @param expressionExecutor Evaluates the expression (post user check evaluation)
     */
    protected <A extends Annotation> ExpressionResult checkPermissions(
            Type<?> resourceClass,
            Class<A> annotationClass,
            Set<String> fields,
            Supplier<Expression> expressionSupplier,
            Function<Expression, ExpressionResult> expressionExecutor) {

        return checkPermissions(
                resourceClass,
                annotationClass,
                fields,
                expressionSupplier,
                Optional.of(expressionExecutor)
        );
    }


    /**
     * Get permission filter on an entity.
     *
     * @param resourceClass Resource class
     * @param requestedFields The set of requested fields
     * @return the filter expression for the class, if any
     */
    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Type<?> resourceClass, Set<String> requestedFields) {
        FilterExpression filterExpression =
                expressionBuilder.buildAnyFieldFilterExpression(resourceClass, requestScope, requestedFields);

        return Optional.ofNullable(filterExpression);
    }

    /**
     * Execute commmit checks.
     */
    @Override
    public void executeCommitChecks() {
        commitCheckQueue.forEach((expr) -> {
            Expression expression = expr.getExpression();
            ExpressionResult result = expression.evaluate(Expression.EvaluationMode.ALL_CHECKS);
            if (result == FAIL) {
                ForbiddenAccessException e = new ForbiddenAccessException(
                        expr.getAnnotationClass(), expression, Expression.EvaluationMode.ALL_CHECKS);
                if (log.isTraceEnabled()) {
                    log.trace("{}", e.getLoggedMessage());
                }
                throw e;
            }
        });
        commitCheckQueue.clear();
    }

    /**
     * Execute expressions.
     *
     * @param expression The expression to evaluate.
     * @param annotationClass The permission associated with the expression.
     * @param mode The evaluation mode of the expression.
     */
    private ExpressionResult executeExpressions(final Expression expression,
                                                final Class<? extends Annotation> annotationClass,
                                                Expression.EvaluationMode mode) {

        ExpressionResult result = expression.evaluate(mode);

        // Record the check
        if (log.isTraceEnabled()) {
            String checkKey = expression.toString();
            Long checkOccurrences = checkStats.getOrDefault(checkKey, 0L) + 1;
            checkStats.put(checkKey, checkOccurrences);
        }

        if (result == DEFERRED) {

            /*
             * Checking user checks only are an optimization step.  We don't need to defer these checks because
             * INLINE_ONLY checks will be evaluated later.  Also, the user checks don't have
             * the correct context to evaluate as COMMIT checks later.
             */
            if (mode == Expression.EvaluationMode.USER_CHECKS_ONLY) {
                return DEFERRED;
            }


            if (isInlineOnlyCheck(annotationClass)) {
                // Force evaluation of checks that can only be executed inline.
                result = expression.evaluate(Expression.EvaluationMode.ALL_CHECKS);
                if (result == FAIL) {
                    ForbiddenAccessException e = new ForbiddenAccessException(
                        annotationClass,
                        expression,
                        Expression.EvaluationMode.ALL_CHECKS);
                    if (log.isTraceEnabled()) {
                        log.trace("{}", e.getLoggedMessage());
                    }
                    throw e;
                }
                return result;
            }
            commitCheckQueue.add(new QueuedCheck(expression, annotationClass));
            return DEFERRED;
        }
        if (result == FAIL) {
            ForbiddenAccessException e = new ForbiddenAccessException(annotationClass, expression, mode);
            if (log.isTraceEnabled()) {
                log.trace("{}", e.getLoggedMessage());
            }
            throw e;
        }

        return result;
    }

    /**
     * Check whether or not this check can only be run inline or not.
     *
     * @param annotationClass annotation class
     * @return True if check can only be run inline, false otherwise.
     */
    private boolean isInlineOnlyCheck(final Class<? extends Annotation> annotationClass) {
        return ReadPermission.class.isAssignableFrom(annotationClass)
                || DeletePermission.class.isAssignableFrom(annotationClass);
    }

    /**
     * Information container about queued checks.
     */
    @AllArgsConstructor
    private static class QueuedCheck {
        @Getter private final Expression expression;
        @Getter private final Class<? extends Annotation> annotationClass;
    }

    /**
     * Logs the permission check statistics.
     *
     */
    @Override
    public void logCheckStats() {
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("Permission Check Statistics:\n");
            checkStats.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEachOrdered(e -> sb.append(e.getKey() + ": " + e.getValue() + "\n"));
            String stats = sb.toString();
            log.trace(stats);
        }
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Check strictly user permissions on an entity field.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param field The entity field
     */
    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        String field) {
        Supplier<Expression> expressionSupplier = () ->
            expressionBuilder.buildUserCheckFieldExpressions(
                    resourceClass,
                    requestScope,
                    annotationClass,
                    field);

        return checkOnlyUserPermissions(
                resourceClass,
                annotationClass,
                Collections.singleton(field),
                expressionSupplier);
    }
}
