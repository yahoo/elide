/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.permissions.ExpressionResult;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Interface describing classes responsible for managing the life-cycle and execution of checks.
 *
 * Checks are expected to throw exceptions upon failures.
 */
public interface PermissionExecutor {
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
    <A extends Annotation> ExpressionResult checkPermission(Class<A> annotationClass, PersistentResource resource);

    /**
     * Check permission on class.
     *
     * @param <A> type parameter
     * @param annotationClass annotation class
     * @param resource resource
     * @param changeSpec ChangeSpec
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    <A extends Annotation> ExpressionResult checkPermission(Class<A> annotationClass,
                                                            PersistentResource resource,
                                                            ChangeSpec changeSpec);

    /**
     * Check for permissions on a specific field.
     *
     * @param <A> type parameter
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     */
    <A extends Annotation> ExpressionResult checkSpecificFieldPermissions(PersistentResource<?> resource,
                                                                          ChangeSpec changeSpec,
                                                                          Class<A> annotationClass,
                                                                          String field);

    /**
     * Check for permissions on a specific field deferring all checks.
     *
     * @param <A> type parameter
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     */
    <A extends Annotation> ExpressionResult checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                                  ChangeSpec changeSpec,
                                                                                  Class<A> annotationClass,
                                                                                  String field);

    /**
     * Check strictly user permissions on a specific field and entity.
     *
     * @param <A> type parameter
     * @param resource Resource
     * @param annotationClass Annotation class
     * @param field Field
     */
    <A extends Annotation> ExpressionResult checkUserPermissions(PersistentResource<?> resource,
                                                                 Class<A> annotationClass,
                                                                 String field);

    /**
     * Check strictly user permissions on an entity.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     */
    <A extends Annotation> ExpressionResult checkUserPermissions(Class<?> resourceClass, Class<A> annotationClass);

    Optional<FilterExpression> getReadPermissionFilter(Class<?> resourceClass);

    /**
     * Execute commmit checks.
     */
    void executeCommitChecks();

    default boolean shouldShortCircuitPermissionChecks(Class<? extends Annotation> annotationClass,
                                               Class resourceClass, String field) {
        return false;
    }

    default String printCheckStats() {
        return null;
    }

    /**
     * Whether or not the permission executor will return verbose logging to the requesting user in the response.
     *
     * @return True if verbose, false otherwise.
     */
    default boolean isVerbose() {
        return false;
    }
}
