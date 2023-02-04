/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.executors;

import static com.yahoo.elide.core.security.permissions.ExpressionResult.DEFERRED;
import static com.yahoo.elide.core.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.core.security.permissions.ExpressionResult.PASS;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.security.permissions.ExpressionResultCache;
import com.yahoo.elide.core.security.permissions.PermissionExpressionBuilder;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.type.Type;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract Permission Executor with common permission executor functionalities.
 */
public abstract class AbstractPermissionExecutor implements PermissionExecutor {
    private final Logger log;
    protected final Queue<AbstractPermissionExecutor.QueuedCheck> commitCheckQueue = new LinkedBlockingQueue<>();

    protected final RequestScope requestScope;
    protected final PermissionExpressionBuilder expressionBuilder;
    protected final Map<Triple<Class<? extends Annotation>, Type, ImmutableSet<String>>, ExpressionResult>
            userPermissionCheckCache;
    protected final Map<String, Long> checkStats;

    public AbstractPermissionExecutor(Logger log, RequestScope requestScope) {
        ExpressionResultCache cache = new ExpressionResultCache();
        this.log = log;
        this.requestScope = requestScope;
        this.expressionBuilder = new PermissionExpressionBuilder(cache, requestScope.getDictionary());
        userPermissionCheckCache = new HashMap<>();
        checkStats = new HashMap<>();
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
     * Execute expressions.
     *
     * @param expression The expression to evaluate.
     * @param annotationClass The permission associated with the expression.
     * @param mode The evaluation mode of the expression.
     */
    protected ExpressionResult executeExpressions(final Expression expression,
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
            commitCheckQueue.add(new AbstractPermissionExecutor.QueuedCheck(expression, annotationClass));
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
        @Getter
        private final Expression expression;
        @Getter private final Class<? extends Annotation> annotationClass;
    }
}
