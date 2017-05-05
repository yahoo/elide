/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.executors;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED;
import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS;

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
    public ActivePermissionExecutor(final com.yahoo.elide.core.RequestScope requestScope) {
        this(false, requestScope);
    }

    /**
     * Constructor.
     *
     * @param verbose True if executor should produce verbose output to caller
     * @param requestScope Request scope
     */
    public ActivePermissionExecutor(boolean verbose, final com.yahoo.elide.core.RequestScope requestScope) {
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
     * Check permission on class.
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
        Expression expression;
        if (SharePermission.class == annotationClass) {
            expression = expressionBuilder.buildSharePermissionExpressions(resource);
        } else {
            expression = expressionBuilder.buildAnyFieldExpressions(resource, annotationClass, changeSpec);

            ExpressionResult expressionResult = this.checkUserPermissions(resource.getResourceClass(),
                    annotationClass, Optional.empty(), expression);
            if (expressionResult == PASS) {
                return expressionResult;
            }
        }
        return executeExpressions(expression, annotationClass, Expression.EvaluationMode.INLINE_CHECKS_ONLY);
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
        Expression expression = expressionBuilder.buildSpecificFieldExpressions(
                resource,
                annotationClass,
                field,
                changeSpec
        );

        ExpressionResult expressionResult = this.checkUserPermissions(resource.getResourceClass(),
                annotationClass, Optional.of(field), expression);
        if (expressionResult == PASS) {
            return expressionResult;
        }

        return executeExpressions(expression, annotationClass, Expression.EvaluationMode.INLINE_CHECKS_ONLY);
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
        Expression expression = expressionBuilder.buildSpecificFieldExpressions(
                resource,
                annotationClass,
                field,
                changeSpec
        );

        ExpressionResult expressionResult = this.checkUserPermissions(resource.getResourceClass(),
                annotationClass, Optional.of(field), expression);

        if (expressionResult == PASS) {
            return expressionResult;
        }

        commitCheckQueue.add(new QueuedCheck(expression, annotationClass));
        return ExpressionResult.DEFERRED;
    }

    /**
     * Check strictly user permissions on a specific field and entity.
     *
     * @param <A> type parameter
     * @param resource Resource
     * @param annotationClass Annotation class
     * @param field Field
     */
    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(PersistentResource<?> resource,
                                                                        Class<A> annotationClass,
                                                                        String field) {
        Expression expression = expressionBuilder.buildUserCheckFieldExpressions(resource, annotationClass, field);
        return checkUserPermissions(resource.getResourceClass(), annotationClass, Optional.of(field), expression);

    }

    /**
     * Check strictly user permissions on an entity.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param field Optional field name that is being accessed
     * @param expression The expression to evaluate as a user check
     */
    protected <A extends Annotation> ExpressionResult checkUserPermissions(Class<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        Optional<String> field,
                                                                        Expression expression) {

        // If the user check has already been evaluated before, return the result directly and save the building cost
        ExpressionResult expressionResult
                = userPermissionCheckCache.get(Triple.of(annotationClass, resourceClass, field.orElse(null)));
        if (expressionResult != null) {
            return expressionResult;
        }

        expressionResult = executeExpressions(expression, annotationClass, Expression.EvaluationMode.USER_CHECKS_ONLY);

        userPermissionCheckCache.put(Triple.of(annotationClass, resourceClass, field.orElse(null)), expressionResult);

        return expressionResult;
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
        Expression expression = expressionBuilder.buildUserCheckAnyExpression(
                resourceClass,
                annotationClass,
                requestScope
        );

        return checkUserPermissions(resourceClass, annotationClass, Optional.empty(), expression);
    }

    /**
     * Get permission filter on an entity.
     *
     * @param resourceClass Resource class
     * @return the filter expression for the class, if any
     */
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
                ForbiddenAccessException e = new ForbiddenAccessException(expr.getAnnotationClass().getSimpleName(),
                        expression, Expression.EvaluationMode.ALL_CHECKS);
                log.trace("{}", e.getLoggedMessage());
                throw e;
            }
        });
        commitCheckQueue.clear();
    }

    @Override
    public boolean shouldShortCircuitPermissionChecks(Class<? extends Annotation> annotationClass,
                                                      Class resourceClass, String field) {
        ExpressionResult result = userPermissionCheckCache.get(Triple.of(annotationClass, resourceClass, field));

        return (result == PASS);
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
                        annotationClass.getSimpleName(),
                        expression,
                        Expression.EvaluationMode.ALL_CHECKS);
                    log.trace("{}", e.getLoggedMessage());
                    throw e;
                }
            } else {
                commitCheckQueue.add(new QueuedCheck(expression, annotationClass));
            }
            return DEFERRED;
        } else if (result == FAIL) {
            ForbiddenAccessException e = new ForbiddenAccessException(annotationClass.getSimpleName(),
                    expression, mode);
            log.trace("{}", e.getLoggedMessage());
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
