/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.executors;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.type.Type;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Permission Executor for all models managed by aggregation datastore.
 */
@Slf4j
public class AggregationStorePermissionExecutor extends AbstractPermissionExecutor {

    public AggregationStorePermissionExecutor(RequestScope requestScope) {
        super(log, requestScope);
    }

    @Override
    public <A extends Annotation> ExpressionResult checkPermission(Class<A> annotationClass, PersistentResource resource, Set<String> requestedFields) {
        return ExpressionResult.PASS;
    }

    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissions(PersistentResource<?> resource, ChangeSpec changeSpec, Class<A> annotationClass, String field) {
        return checkUserPermissions(resource.getResourceType(), annotationClass, field);
    }

    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource, ChangeSpec changeSpec, Class<A> annotationClass, String field) {
        return null;
    }

    /**
     * Check strictly user permissions on an entity.
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     */
    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass, Class<A> annotationClass, Set<String> requestedFields) {
        Supplier<Expression> expressionSupplier = () ->
                expressionBuilder.buildUserCheckAnyExpression(
                        resourceClass,
                        annotationClass,
                        requestedFields,
                        requestScope);

        return checkOnlyUserPermissions(
                resourceClass,
                annotationClass,
                Optional.empty(),
                expressionSupplier);
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
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass, Class<A> annotationClass, String field) {
        Supplier<Expression> expressionSupplier = () ->
                expressionBuilder.buildUserCheckFieldExpressions(
                        resourceClass,
                        requestScope,
                        annotationClass,
                        field);

        return checkOnlyUserPermissions(
                resourceClass,
                annotationClass,
                Optional.of(field),
                expressionSupplier);
    }

    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Type<?> resourceClass, Set<String> requestedFields) {
        FilterExpression filterExpression = expressionBuilder.buildEntityFilterExpression(resourceClass, requestScope);
        return Optional.ofNullable(filterExpression);
    }
}
