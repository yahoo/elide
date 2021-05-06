/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.executors;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.type.Type;

import lombok.AllArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * MultiplexPermissionExecutor manages Model to permssion executor mapping.
 * All method call to multiple permission executor will be delegated
 *  to the underlying permission executor based on resource type.
 */
@AllArgsConstructor
public class MultiplexPermissionExecutor implements PermissionExecutor {

    private Map<Type<?>, PermissionExecutor> permissionExecutorMap;
    private PermissionExecutor defaultPermissionExecutor;
    private EntityDictionary dictionary;

    public PermissionExecutor getPermissionExecutor(Type<?> cls) {
        return permissionExecutorMap.getOrDefault(dictionary.lookupBoundClass(cls), defaultPermissionExecutor);
    }

    @Override
    public <A extends Annotation> ExpressionResult checkPermission(Class<A> annotationClass,
                                                                   PersistentResource resource,
                                                                   Set<String> requestedFields) {
        return getPermissionExecutor(resource.getResourceType())
                .checkPermission(annotationClass, resource, requestedFields);
    }

    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissions(PersistentResource<?> resource,
                                                                                 ChangeSpec changeSpec,
                                                                                 Class<A> annotationClass,
                                                                                 String field) {
        return getPermissionExecutor(resource.getResourceType())
                .checkSpecificFieldPermissions(resource, changeSpec, annotationClass, field);
    }

    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                                         ChangeSpec changeSpec,
                                                                                         Class<A> annotationClass,
                                                                                         String field) {
        return getPermissionExecutor(resource.getResourceType())
                .checkSpecificFieldPermissionsDeferred(resource, changeSpec, annotationClass, field);
    }

    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        Set<String> requestedFields) {
        return getPermissionExecutor(resourceClass)
                .checkUserPermissions(resourceClass, annotationClass, requestedFields);
    }

    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        String field) {
        return getPermissionExecutor(resourceClass)
                .checkUserPermissions(resourceClass, annotationClass, field);
    }

    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Type<?> resourceClass, Set<String> requestedFields) {
        return getPermissionExecutor(resourceClass).getReadPermissionFilter(resourceClass, requestedFields);
    }

    @Override
    public void executeCommitChecks() {
        defaultPermissionExecutor.executeCommitChecks();
        permissionExecutorMap.values().forEach(PermissionExecutor::executeCommitChecks);
    }

    @Override
    public void logCheckStats() {
        defaultPermissionExecutor.logCheckStats();
        permissionExecutorMap.values().forEach(PermissionExecutor::logCheckStats);
    }

    @Override
    public boolean isVerbose() {
        return defaultPermissionExecutor.isVerbose();
    }

    @Override
    public ExpressionResult evaluateFilterJoinUserChecks(PersistentResource<?> resource,
                                                         FilterPredicate filterPredicate) {
        return getPermissionExecutor(resource.getResourceType())
                .evaluateFilterJoinUserChecks(resource, filterPredicate);
    }

    @Override
    public ExpressionResult handleFilterJoinReject(FilterPredicate filterPredicate,
                                                   Path.PathElement pathElement,
                                                   ForbiddenAccessException reason) {
        return getPermissionExecutor(pathElement.getType())
                .handleFilterJoinReject(filterPredicate, pathElement, reason);
    }
}
