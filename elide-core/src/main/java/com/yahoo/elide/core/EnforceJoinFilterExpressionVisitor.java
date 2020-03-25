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
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Enforce read permission on filter join
 */
public class EnforceJoinFilterExpressionVisitor implements FilterExpressionVisitor<Boolean> {
    private PersistentResource<?> resource;

    public EnforceJoinFilterExpressionVisitor(PersistentResource<?> resource) {
        this.resource = resource;
    }

    @Override
    public Boolean visitPredicate(FilterPredicate filterPredicate) {
        RequestScope requestScope = resource.getRequestScope();

        // enforce filter checking only when user filter is present
        if (!requestScope.getQueryParams().isPresent()
                || !requestScope.getQueryParams().get().keySet().stream().anyMatch(k -> k.startsWith("filter["))) {
            return true;
        }
        Set<PersistentResource> val = Collections.singleton(resource);
        for (Path.PathElement pathElement : filterPredicate.getPath().getPathElements()) {
            Class<?> entityClass = pathElement.getType();
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
            } catch (IllegalArgumentException e) {
                // Not a persistent resource
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
        Object obj = PersistentResource.getValue(resource.getObject(), fieldName, requestScope);
        PersistentResourceSet persistentResourceSet;
        if (obj instanceof Iterable) {
            persistentResourceSet = new PersistentResourceSet(resource, (Iterable) obj, requestScope);
        } else if (obj != null) {
            persistentResourceSet = new PersistentResourceSet(resource, Collections.singleton(obj), requestScope);
        } else {
            return Stream.empty();
        }

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(persistentResourceSet.iterator(), 0), false);
    }

    @Override
    public Boolean visitAndExpression(AndFilterExpression expression) {
        Boolean left = expression.getLeft().accept(this);
        Boolean right = expression.getRight().accept(this);
        // neither rejected
        return left && right;
    }

    @Override
    public Boolean visitOrExpression(OrFilterExpression expression) {
        Boolean left = expression.getLeft().accept(this);
        Boolean right = expression.getRight().accept(this);
        // neither rejected
        return left && right;
    }

    @Override
    public Boolean visitNotExpression(NotFilterExpression expression) {
        // check rejected
        return expression.getNegated().accept(this);
    }
}
