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
     * @return the results of evaluating the permission
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
     * @return the results of evaluating the permission
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
     * @return the results of evaluating the permission
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
     * @return the results of evaluating the permission
     */
    <A extends Annotation> ExpressionResult checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                                  ChangeSpec changeSpec,
                                                                                  Class<A> annotationClass,
                                                                                  String field);

    /**
     * Check strictly user permissions on an entity.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @return the results of evaluating the permission
     */
    <A extends Annotation> ExpressionResult checkUserPermissions(Class<?> resourceClass, Class<A> annotationClass);

    /**
     * Get the read filter, if defined.
     *
     * @param resourceClass the class to check for a filter
     * @return the an optional containg the filter
     */
    Optional<FilterExpression> getReadPermissionFilter(Class<?> resourceClass);

    /**
     * Execute commit checks.
     */
    void executeCommitChecks();

    /**
     * Return useful information about the check evaluation.
     *
     * @return a description describing check execution
     */
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
