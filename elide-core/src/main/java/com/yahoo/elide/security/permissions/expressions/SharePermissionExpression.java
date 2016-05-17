/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.PermissionCondition;

import java.util.Optional;

import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;

/**
 * Determines whether a resource is shareable.
 */
public class SharePermissionExpression implements Expression {
    private final Optional<Expression> entityExpression;
    private final PermissionCondition condition;

    public SharePermissionExpression(final PermissionCondition condition,
                                     final Expression entityExpression) {
        this.condition = condition;
        this.entityExpression = Optional.of(entityExpression);
    }

    public SharePermissionExpression(final PermissionCondition condition) {
        this.condition = condition;
        this.entityExpression = Optional.ofNullable(null);
    }

    @Override
    public ExpressionResult evaluate() {
        PersistentResource resource = condition.getResource().get();
        EntityDictionary dictionary = ((com.yahoo.elide.core.PersistentResource) resource).getDictionary();

        if (!dictionary.isShareable(resource.getResourceClass()) || !entityExpression.isPresent()) {
            return FAIL;
        }

        return entityExpression.get().evaluate();
    }

    @Override
    public String toString() {
        String entityText = entityExpression.isPresent() ? entityExpression.get().toString() : "NOT MARKED SHAREABLE";
        return String.format("%s FOR EXPRESSION [SHARE ENTITY(%s)]", condition, entityText);
    }
}
