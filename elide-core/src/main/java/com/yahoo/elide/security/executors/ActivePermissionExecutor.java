/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.executors;

import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.SecurityMode;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.permissions.ExpressionBuilder;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.expressions.Expression;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.yahoo.elide.security.permissions.ExpressionResult.Status.DEFERRED;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.FAIL;

/**
 * Default permission executor.
 * This executor executes all security checks as outlined in the documentation.
 */
public class ActivePermissionExecutor implements PermissionExecutor {
    private final Queue<QueuedCheck> commitCheckQueue = new LinkedBlockingQueue<>();

    private final RequestScope requestScope;
    private final ExpressionBuilder expressionBuilder;

    /**
     * Constructor.
     *
     * @param requestScope Request scope.
     */
    public ActivePermissionExecutor(final com.yahoo.elide.core.RequestScope requestScope) {
        HashMap<Class<? extends Check>, Map<PersistentResource, ExpressionResult>> cache = new HashMap<>();

        this.requestScope = requestScope;
        this.expressionBuilder = new ExpressionBuilder(cache, requestScope.getDictionary());
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
    @Override
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
    @Override
    public <A extends Annotation> void checkPermission(Class<A> annotationClass,
                                                       PersistentResource resource,
                                                       ChangeSpec changeSpec) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        checkIsValidSharePermission(annotationClass, resource);
        ExpressionBuilder.Expressions expressions =
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
    @Override
    public <A extends Annotation> void checkSpecificFieldPermissions(PersistentResource<?> resource,
                                                                     ChangeSpec changeSpec,
                                                                     Class<A> annotationClass,
                                                                     String field) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        checkIsValidSharePermission(annotationClass, resource);
        ExpressionBuilder.Expressions expressions =
                expressionBuilder.buildSpecificFieldExpressions(resource, annotationClass, field, changeSpec);
        executeExpressions(expressions, annotationClass);
    }

    /**
     * Check for permissions on a specific field deferring all checks.
     *
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     * @param <A> type parameter
     */
    @Override
    public <A extends Annotation> void checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                             ChangeSpec changeSpec,
                                                                             Class<A> annotationClass,
                                                                             String field) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        checkIsValidSharePermission(annotationClass, resource);
        ExpressionBuilder.Expressions expressions =
                expressionBuilder.buildSpecificFieldExpressions(resource, annotationClass, field, changeSpec);
        Expression commitExpression = expressions.getCommitExpression();
        if (commitExpression != null) {
            commitCheckQueue.add(new QueuedCheck(commitExpression, annotationClass));
        }
    }

    /**
     * Check strictly user permissions on a specific field and entity.
     *
     * @param resource Resource
     * @param annotationClass Annotation class
     * @param field Field
     * @param <A> type parameter
     */
    @Override
    public <A extends Annotation> void checkUserPermissions(PersistentResource<?> resource,
                                                            Class<A> annotationClass,
                                                            String field) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        checkIsValidSharePermission(annotationClass, resource);
        ExpressionBuilder.Expressions expressions =
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
    @Override
    public <A extends Annotation> void checkUserPermissions(Class<?> resourceClass,
                                                            Class<A> annotationClass) {
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_INACTIVE) {
            return; // Bypass
        }
        checkIsValidSharePermission(annotationClass, resourceClass);
        ExpressionBuilder.Expressions expressions =
                expressionBuilder.buildUserCheckAnyExpression(resourceClass, annotationClass, requestScope);
        executeExpressions(expressions, annotationClass);
    }

    /**
     * Execute commmit checks.
     */
    @Override
    public void executeCommitChecks() {
        commitCheckQueue.forEach((expr) -> {
            ExpressionResult result = expr.getExpression().evaluate();
            if (result.getStatus() == FAIL) {
                throw new ForbiddenAccessException(expr.getAnnotationClass().getSimpleName()
                        + " " + result.getFailureMessage());
            }
        });
        commitCheckQueue.clear();
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
     * Check whether or not a permission is a share permission.
     *   (1) If the check is SharePermission, then ensure that the resource is shareable
     *       (a) If resource is shareable, return from method and run checks as expected
     *       (b) If resource is not shareable, throw forbidden access exception
     *   (2) If the check is not SharePermission, return from method and run checks as expected
     *
     * NOTE: This method exists because the default behavior for SharePermission is to FAIL for security concerns.
     *
     * @param annotationClass Annotation class
     * @param resource persistent resource
     * @param <A> type parameter
     */
    private <A extends Annotation> void checkIsValidSharePermission(Class<A> annotationClass,
                                                                    PersistentResource resource) {
        if (SharePermission.class == annotationClass
                && !requestScope.getDictionary().isShareable(resource.getResourceClass())) {
            requestScope.logAuthFailure(null, resource.getType(), resource.getId());
            throw new ForbiddenAccessException("Resource Not Shareable");
        }
    }

    /**
     * Check whether or not a permission is a share permission.
     *   (1) If the check is SharePermission, then ensure that the resource is shareable
     *       (a) If resource is shareable, return from method and run checks as expected
     *       (b) If resource is not shareable, throw forbidden access exception
     *   (2) If the check is not SharePermission, return from method and run checks as expected
     *
     * NOTE: This method exists because the default behavior for SharePermission is to FAIL for security concerns.
     *
     * @param annotationClass Annotation class
     * @param resourceClass resource class
     * @param <A> type parameter
     */
    private <A extends Annotation> void checkIsValidSharePermission(Class<A> annotationClass,
                                                                    Class<?> resourceClass) {
        if (SharePermission.class == annotationClass && !requestScope.getDictionary().isShareable(resourceClass)) {
            requestScope.logAuthFailure(null, resourceClass.toString(), "unknown");
            throw new ForbiddenAccessException("Resource Not Shareable");
        }
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
