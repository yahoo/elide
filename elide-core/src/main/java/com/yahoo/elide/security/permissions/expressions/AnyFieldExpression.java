/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS;

import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.PermissionCondition;

/**
 * This check determines if an entity is accessible to the current user.
 *
 * An entity is considered to be accessible if there exists an annotation at _any_ level of the object that
 * Grants access. This means that if access is permitted to any field of the object then the object
 * is accessible, regardless of what any class or package level permissions would permit.
 */
public class AnyFieldExpression implements Expression {
    private final Expression entityExpression;
    private final Expression fieldExpression;
    private final PermissionCondition condition;

    public AnyFieldExpression(final PermissionCondition condition,
                              final Expression entityExpression,
                              final OrExpression fieldExpression) {
        this.condition = condition;
        this.entityExpression = entityExpression;
        this.fieldExpression = fieldExpression;
    }

    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        ExpressionResult fieldResult = fieldExpression.evaluate(mode);

        if (fieldResult != FAIL) {
            return fieldResult;
        }

        ExpressionResult entityResult = (entityExpression == null) ? PASS : entityExpression.evaluate(mode);
        return entityResult;
    }

    @Override
    public String toString() {
        return String.format("%s FOR EXPRESSION [(FIELDS(%s)) OR (ENTITY(%s))]",
                condition, fieldExpression, entityExpression);
    }
}
