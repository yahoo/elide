/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.executors;

import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED;
import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.ExpressionResultCache;
import com.yahoo.elide.security.permissions.PermissionExpressionBuilder;
import com.yahoo.elide.security.permissions.expressions.Expression;

import org.apache.commons.lang3.tuple.Triple;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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
    private final Map<Triple<Class<? extends Annotation>, Class, String>, ExpressionResult> userPermissionCheckCache;
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

    /**
     * Check permission on class.
     *
     * @param <A> type parameter
     * @param annotationClass annotation class
     * @param resource resource
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    @Override
    public <A extends Annotation> ExpressionResult checkPermission(
            Class<A> annotationClass, PersistentResource resource) {
        return checkPermission(annotationClass, resource, null);
    }

    /**
     * Check permission on class. Checking on SharePermission falls to check ReadPermission.
     *
     * @param annotationClass annotation class
     * @param resource resource
     * @param changeSpec ChangeSpec
     * @param <A> type parameter
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    @Override
    public <A extends Annotation> ExpressionResult checkPermission(Class<A> annotationClass,
                                                                   PersistentResource resource,
                                                                   ChangeSpec changeSpec) {
        Supplier<Expression> expressionSupplier = () -> {
            if (SharePermission.class == annotationClass) {
                if (requestScope.getDictionary().isShareable(resource.getResourceClass())) {
                    return expressionBuilder.buildAnyFieldExpressions(resource, ReadPermission.class, changeSpec);
                } else {
                    return PermissionExpressionBuilder.FAIL_EXPRESSION;
                }
            }
            return expressionBuilder.buildAnyFieldExpressions(resource, annotationClass, changeSpec);
        };

        Function<Expression, ExpressionResult> expressionExecutor = (expression) -> {
            // for newly created object in PatchRequest limit to User checks
            if (requestScope.getNewPersistentResources().contains(resource)) {
                return executeUserChecksDeferInline(annotationClass, expression);
            }
            return executeExpressions(expression, annotationClass, Expression.EvaluationMode.INLINE_CHECKS_ONLY);
        };

        return checkPermissions(
                resource.getResourceClass(),
                annotationClass,
                Optional.empty(),
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
        Supplier<Expression> expressionSupplier = () -> {
            return expressionBuilder.buildSpecificFieldExpressions(resource, annotationClass, field, changeSpec);
        };

        Function<Expression, ExpressionResult> expressionExecutor = (expression) -> {
            return executeExpressions(expression, annotationClass, Expression.EvaluationMode.INLINE_CHECKS_ONLY);
        };

        return checkPermissions(
                resource.getResourceClass(),
                annotationClass,
                Optional.of(field),
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

        Supplier<Expression> expressionSupplier = () -> {
                return expressionBuilder.buildSpecificFieldExpressions(resource,
                        expressionAnnotation,
                        field,
                        changeSpec);
        };

        Function<Expression, ExpressionResult> expressionExecutor = (expression) -> {
            if (requestScope.getNewPersistentResources().contains(resource)) {
                return executeUserChecksDeferInline(expressionAnnotation, expression);
            }
            return executeExpressions(expression, expressionAnnotation, Expression.EvaluationMode.INLINE_CHECKS_ONLY);
        };

        return checkPermissions(
                resource.getResourceClass(),
                expressionAnnotation,
                Optional.of(field),
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
    public <A extends Annotation> ExpressionResult checkUserPermissions(Class<?> resourceClass,
                                                                        Class<A> annotationClass) {
        Supplier<Expression> expressionSupplier = () -> {
            return expressionBuilder.buildUserCheckAnyExpression(
                    resourceClass,
                    annotationClass,
                    requestScope);
        };

        return checkOnlyUserPermissions(
                resourceClass,
                annotationClass,
                Optional.empty(),
                expressionSupplier);
    }


    /**
     * First attempts to check user permissions (by looking in the cache and if not present by executing user
     * permissions).  If user permissions don't short circuit the check, run the provided expression executor.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param field Optional field name that is being accessed
     * @param expressionSupplier Builds a permission expression.
     * @param expressionExecutor Evaluates the expression (post user check evaluation)
     */
    protected <A extends Annotation> ExpressionResult checkPermissions(
            Class<?> resourceClass,
            Class<A> annotationClass,
            Optional<String> field,
            Supplier<Expression> expressionSupplier,
            Optional<Function<Expression, ExpressionResult>> expressionExecutor) {

        // If the user check has already been evaluated before, return the result directly and save the building cost
        ExpressionResult expressionResult
                = userPermissionCheckCache.get(Triple.of(annotationClass, resourceClass, field.orElse(null)));

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
                    Triple.of(annotationClass, resourceClass, field.orElse(null)), expressionResult);

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
     * @param field Optional field name that is being accessed
     * @param expressionSupplier Builds a permission expression.
     */
    protected <A extends Annotation> ExpressionResult checkOnlyUserPermissions(
            Class<?> resourceClass,
            Class<A> annotationClass,
            Optional<String> field,
            Supplier<Expression> expressionSupplier) {

        return checkPermissions(
                resourceClass,
                annotationClass,
                field,
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
     * @param field Optional field name that is being accessed
     * @param expressionSupplier Builds a permission expression.
     * @param expressionExecutor Evaluates the expression (post user check evaluation)
     */
    protected <A extends Annotation> ExpressionResult checkPermissions(
            Class<?> resourceClass,
            Class<A> annotationClass,
            Optional<String> field,
            Supplier<Expression> expressionSupplier,
            Function<Expression, ExpressionResult> expressionExecutor) {

        return checkPermissions(
                resourceClass,
                annotationClass,
                field,
                expressionSupplier,
                Optional.of(expressionExecutor)
        );
    }


    /**
     * Get permission filter on an entity.
     *
     * @param resourceClass Resource class
     * @return the filter expression for the class, if any
     */
    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Class<?> resourceClass) {
        FilterExpression filterExpression =
                expressionBuilder.buildAnyFieldFilterExpression(resourceClass, requestScope);

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
                        EntityDictionary.getSimpleName(expr.getAnnotationClass()),
                        expression, Expression.EvaluationMode.ALL_CHECKS);
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
                        EntityDictionary.getSimpleName(annotationClass),
                        expression,
                        Expression.EvaluationMode.ALL_CHECKS);
                    if (log.isTraceEnabled()) {
                        log.trace("{}", e.getLoggedMessage());
                    }
                    throw e;
                }
            } else {
                commitCheckQueue.add(new QueuedCheck(expression, annotationClass));
            }
            return DEFERRED;
        }
        if (result == FAIL) {
            ForbiddenAccessException e = new ForbiddenAccessException(
                    EntityDictionary.getSimpleName(annotationClass), expression, mode);
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
     * Print the permission check statistics.
     *
     * @return the permission check statistics
     */
    @Override
    public String printCheckStats() {
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("Permission Check Statistics:\n");
            checkStats.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEachOrdered(e -> sb.append(e.getKey() + ": " + e.getValue() + "\n"));
            String stats = sb.toString();
            log.trace(stats);
            return stats;
        }
        return null;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }
}
