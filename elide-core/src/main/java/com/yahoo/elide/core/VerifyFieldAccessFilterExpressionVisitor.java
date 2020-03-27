/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        for (Path.PathElement pathElement : filterPredicate.getPath().getPathElements()) {
            String fieldName = pathElement.getFieldName();

            if ("this".equals(fieldName)) {
                continue;
            }

            try {
                val = val.stream()
                        .filter(Objects::nonNull)
                        .flatMap(x -> getValueChecked(x, fieldName, requestScope))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            } catch (ForbiddenAccessException e) {
                return false;
            }
        }
        return true;
    }

    private Stream<PersistentResource> getValueChecked(PersistentResource<?> resource, String fieldName,
            RequestScope requestScope) {
        // checkFieldAwareReadPermissions
        requestScope.getPermissionExecutor().checkSpecificFieldPermissions(resource, null, ReadPermission.class,
                fieldName);
        Object entity = resource.getObject();
        if (entity == null || resource.getDictionary()
                .getRelationshipType(entity.getClass(), fieldName) == RelationshipType.NONE) {
            return Stream.empty();
        }
        Optional<FilterExpression> filterExpression = requestScope.getExpressionForRelation(resource, fieldName);
        return resource.getRelationChecked(fieldName, filterExpression, Optional.empty(), Optional.empty()).stream();
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
