/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.visitors;

import com.paiondata.elide.annotation.ReadPermission;
import com.paiondata.elide.core.Path.PathElement;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.RelationshipType;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpressionVisitor;
import com.paiondata.elide.core.filter.expression.NotFilterExpression;
import com.paiondata.elide.core.filter.expression.OrFilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.security.PermissionExecutor;
import com.paiondata.elide.core.security.permissions.ExpressionResult;

import io.reactivex.Observable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforce read permission on filter join.
 */
public class VerifyFieldAccessFilterExpressionVisitor implements FilterExpressionVisitor<Boolean> {
    private PersistentResource<?> resource;

    public VerifyFieldAccessFilterExpressionVisitor(PersistentResource<?> resource) {
        this.resource = resource;
    }

    /**
     * Enforce ReadPermission on provided query filter.
     *
     * @return true if allowed, false if rejected
     */
    @Override
    public Boolean visitPredicate(FilterPredicate filterPredicate) {
        RequestScope requestScope = resource.getRequestScope();
        Set<PersistentResource> val = Collections.singleton(resource);

        PermissionExecutor permissionExecutor = requestScope.getPermissionExecutor();

        ExpressionResult result = permissionExecutor.evaluateFilterJoinUserChecks(resource, filterPredicate);

        if (result == ExpressionResult.UNEVALUATED) {
            result = evaluateUserChecks(filterPredicate, permissionExecutor);
        }
        if (result == ExpressionResult.PASS) {
            return true;
        }
        if (result == ExpressionResult.FAIL) {
            return false;
        }

        for (PathElement element : filterPredicate.getPath().getPathElements()) {
            String fieldName = element.getFieldName();

            if ("this".equals(fieldName)) {
                continue;
            }

            try {
                val = val.stream()
                        .filter(Objects::nonNull)
                        .flatMap(x ->
                                getValueChecked(x, fieldName, requestScope)
                                        .toList(LinkedHashSet::new)
                                        .blockingGet()
                                        .stream())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            } catch (ForbiddenAccessException e) {
                result = permissionExecutor.handleFilterJoinReject(filterPredicate, element, e);
                if (result == ExpressionResult.DEFERRED) {
                    continue;
                }
                // pass or fail
                return result == ExpressionResult.PASS;
            }
        }
        return true;
    }

    private Observable<PersistentResource> getValueChecked(PersistentResource<?> resource, String fieldName,
                                                           RequestScope requestScope) {

        EntityDictionary dictionary = resource.getDictionary();

        // checkFieldAwareReadPermissions
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class,
                fieldName);
        Object entity = resource.getObject();
        if (entity == null || resource.getDictionary()
                .getRelationshipType(resource.getResourceType(), fieldName) == RelationshipType.NONE) {
            return Observable.empty();
        }

        Relationship relationship = Relationship.builder()
                .name(fieldName)
                .alias(fieldName)
                .projection(EntityProjection.builder()
                        .type(dictionary.getParameterizedType(resource.getResourceType(), fieldName))
                        .build())
                .build();
        // use no filter to allow the read directly from loaded resource
        return resource.getRelationChecked(relationship);
    }

    /**
     * Scan the Path for user checks.
     * <ol>
     * <li>If all are PASS, return PASS
     * <li>If any FAIL, return FAIL
     * <li>Otherwise return DEFERRED
     * </ol>
     * @param filterPredicate filterPredicate
     * @param permissionExecutor permissionExecutor
     * @return ExpressionResult
     */
    private ExpressionResult evaluateUserChecks(FilterPredicate filterPredicate,
            PermissionExecutor permissionExecutor) {
        PermissionExecutor executor = resource.getRequestScope().getPermissionExecutor();

        ExpressionResult ret = ExpressionResult.PASS;
        for (PathElement element : filterPredicate.getPath().getPathElements()) {
            ExpressionResult result;
            try {
                result = executor.checkUserPermissions(
                        element.getType(),
                        ReadPermission.class,
                        element.getFieldName());
            } catch (ForbiddenAccessException e) {
                result = permissionExecutor.handleFilterJoinReject(filterPredicate, element, e);
            }

            if (result == ExpressionResult.FAIL) {
                return ExpressionResult.FAIL;
            }

            if (result != ExpressionResult.PASS) {
                ret = ExpressionResult.DEFERRED;
            }
        }
        return ret;
    }

    @Override
    public Boolean visitAndExpression(AndFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        // are both allowed
        return left.accept(this) && right.accept(this);
    }

    @Override
    public Boolean visitOrExpression(OrFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        // are both allowed
        return left.accept(this) && right.accept(this);
    }

    @Override
    public Boolean visitNotExpression(NotFilterExpression expression) {
        // is negated expression allowed
        return expression.getNegated().accept(this);
    }
}
