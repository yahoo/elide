/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.executors;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.type.Type;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Collections;
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

    /**
     * Checks user checks for the requested fields.
     * expression = (field1Rule OR field2Rule ... OR fieldNRule)
     * @param annotationClass annotation class
     * @param resource resource
     * @param requestedFields the list of requested fields
     * @param <A>
     * @return ExpressionResult - result of the above any field expression
     */
    @Override
    public <A extends Annotation> ExpressionResult checkPermission(Class<A> annotationClass,
                                                                   PersistentResource resource,
                                                                   Set<String> requestedFields) {
        if (!annotationClass.equals(ReadPermission.class)) {
            return ExpressionResult.FAIL;
        }

        Supplier<Expression> expressionSupplier = () ->
                expressionBuilder.buildUserCheckAnyFieldOnlyExpression(
                        resource.getResourceType(),
                        annotationClass,
                        requestedFields,
                        requestScope);

        return checkOnlyUserPermissions(
                resource.getResourceType(),
                annotationClass,
                requestedFields,
                expressionSupplier);
    }


    /**
     * Evaluates user check permission on specific field
     * Aggregation Datastore model can only have user checks at field level permission expression.
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     * @param <A>
     * @return
     */
    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissions(PersistentResource<?> resource,
                                                                                 ChangeSpec changeSpec,
                                                                                 Class<A> annotationClass,
                                                                                 String field) {
        if (!annotationClass.equals(ReadPermission.class)) {
            return ExpressionResult.FAIL;
        }
        return checkUserPermissions(resource.getResourceType(), annotationClass, field);
    }

    /**
     * Not supported in aggregation datastore.
     * @param resource resource
     * @param changeSpec changepsec
     * @param annotationClass annotation class
     * @param field field to check
     * @param <A>
     * @return
     */
    @Override
    public <A extends Annotation> ExpressionResult checkSpecificFieldPermissionsDeferred(PersistentResource<?> resource,
                                                                                         ChangeSpec changeSpec,
                                                                                         Class<A> annotationClass,
                                                                                         String field) {
        return checkSpecificFieldPermissions(resource, changeSpec, annotationClass, field);
    }

    /**
     * Check strictly user permissions on an entity and any of the requested field. Evaluates
     * expression = (entityRule AND (field1Rule OR field2Rule ... OR fieldNRule))
     *
     * @param <A> type parameter
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     */
    @Override
    public <A extends Annotation> ExpressionResult checkUserPermissions(Type<?> resourceClass,
                                                                        Class<A> annotationClass,
                                                                        Set<String> requestedFields) {
        if (!annotationClass.equals(ReadPermission.class)) {
            return ExpressionResult.FAIL;
        }

        Expression expression = expressionBuilder.buildUserCheckEntityAndAnyFieldExpression(
                        resourceClass,
                        annotationClass,
                        requestedFields,
                        requestScope);

        // Skips userPermissionCheckCache and directly executes the expression.
        // Cache is used by checkPermission() which is called for every resource returned.
        // Cache cannot be shared b/w checkUserPermissions and checkPermissions because of difference in computation.
        return executeExpressions(expression, annotationClass, Expression.EvaluationMode.USER_CHECKS_ONLY);
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
        if (!annotationClass.equals(ReadPermission.class)) {
            return ExpressionResult.FAIL;
        }

        Supplier<Expression> expressionSupplier = () ->
                expressionBuilder.buildUserCheckFieldExpressions(
                        resourceClass,
                        requestScope,
                        annotationClass,
                        field,
                        false);


        return checkOnlyUserPermissions(
                resourceClass,
                annotationClass,
                Collections.singleton(field),
                expressionSupplier);
    }

    @Override
    public Optional<FilterExpression> getReadPermissionFilter(Type<?> resourceClass, Set<String> requestedFields) {
        FilterExpression filterExpression = expressionBuilder.buildEntityFilterExpression(resourceClass, requestScope);
        return Optional.ofNullable(filterExpression);
    }
}
