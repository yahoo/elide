/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import static com.yahoo.elide.security.permissions.ExpressionResult.Status.DEFERRED;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.FAIL;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.ExtractedChecks;
import com.yahoo.elide.security.permissions.ExpressionBuilder;
import com.yahoo.elide.security.permissions.ExpressionBuilder.Expressions;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.expressions.Expression;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class responsible for managing the life-cycle and execution of checks.
 */
public class PermissionExecutor {
    private final Queue<QueuedCheck> commitCheckQueue = new LinkedBlockingQueue<>();

    private final RequestScope requestScope;
    private final ExpressionBuilder expressionBuilder;

    /**
     * Constructor.
     *
     * @param requestScope Request scope.
     */
    public PermissionExecutor(final com.yahoo.elide.core.RequestScope requestScope) {
        HashMap<Class<? extends Check>, Map<PersistentResource, ExpressionResult>> cache = new HashMap<>();

        this.requestScope = requestScope;
        this.expressionBuilder = new ExpressionBuilder(cache, requestScope.getDictionary());
    }

    /**
     * Load checks from an entity.
     *
     * @param annotationClass Annotation class
     * @param resourceClass Resource claSS
     * @param dictionary Dictionary
     * @param <A> type parameter
     * @return Set of extracted checks
     */
    public static <A extends Annotation> ExtractedChecks loadEntityChecks(Class<A> annotationClass,
                                                                          Class<?> resourceClass,
                                                                          EntityDictionary dictionary) {
        return new ExtractedChecks(resourceClass, dictionary, annotationClass);
    }

    /**
     * Check permission on class.
     *
     * @param annotationClass annotation class
     * @param resource resource
     * @param <A> type parameter
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    public <A extends Annotation> void checkPermission(Class<A> annotationClass, PersistentResource resource) {
        checkPermission(annotationClass, resource, null);
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
    public <A extends Annotation> void checkPermission(Class<A> annotationClass,
                                                       PersistentResource resource,
                                                       ChangeSpec changeSpec) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        Expressions expressions =
                expressionBuilder.buildAnyFieldExpressions(resource, annotationClass, changeSpec);
        executeExpressions(expressions, annotationClass);
    }

    /**
     * Check for permissions on a specific field.
     *
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     * @param <A> type parameter
     */
    public <A extends Annotation> void checkSpecificFieldPermissions(PersistentResource<?> resource,
                                                                     ChangeSpec changeSpec,
                                                                     Class<A> annotationClass,
                                                                     String field) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        Expressions expressions =
                expressionBuilder.buildSpecificFieldExpressions(resource, annotationClass, field, changeSpec);
        executeExpressions(expressions, annotationClass);
    }

    /**
     * Check strictly user permissions on a specific field and entity.
     *
     * @param resource Resource
     * @param annotationClass Annotation class
     * @param field Field
     * @param <A> type parameter
     */
    public <A extends Annotation> void checkUserPermissions(PersistentResource<?> resource,
                                                            Class<A> annotationClass,
                                                            String field) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        Expressions expressions =
                expressionBuilder.buildUserCheckFieldExpressions(resource, annotationClass, field);
        executeExpressions(expressions, annotationClass);
    }

    /**
     * Check strictly user permissions on an entity.
     *
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param <A> type parameter
     */
    public <A extends Annotation> void checkUserPermissions(Class<?> resourceClass,
                                                            Class<A> annotationClass) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        Expressions expressions =
                expressionBuilder.buildUserCheckAnyExpression(resourceClass, annotationClass, requestScope);
        executeExpressions(expressions, annotationClass);
    }

    /**
     * Execute commmit checks.
     */
    public void executeCommitChecks() {
        commitCheckQueue.forEach((expr) -> {
            ExpressionResult result = expr.getExpression().evaluate();
            if (result.getStatus() == FAIL) {
                throw new ForbiddenAccessException(expr.getAnnotationClass().getSimpleName()
                        + " " + result.getFailureMessage());
            }
        });
    }

    /**
     * Execute expressions.
     *
     * @param expressions expressions to execute
     */
    private void executeExpressions(final ExpressionBuilder.Expressions expressions,
                                    final Class<? extends Annotation> annotationClass) {
        ExpressionResult result = expressions.getOperationExpression().evaluate();
        if (result.getStatus() == DEFERRED) {
            Expression commitExpression = expressions.getCommitExpression();
            if (commitExpression != null) {
                commitCheckQueue.add(new QueuedCheck(commitExpression, annotationClass));
            }
        } else if (result.getStatus() == FAIL) {
            throw new ForbiddenAccessException(annotationClass.getSimpleName() + " " + result.getFailureMessage());
        }
    }

    /**
     * Information container about queued checks.
     */
    @AllArgsConstructor
    private static class QueuedCheck {
        @Getter private final Expression expression;
        @Getter private final Class<? extends Annotation> annotationClass;
    }
}
