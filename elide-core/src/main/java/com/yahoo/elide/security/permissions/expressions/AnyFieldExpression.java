/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.permissions.ExpressionResult;

import static com.yahoo.elide.security.permissions.ExpressionResult.PASS_RESULT;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.FAIL;

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

    public AnyFieldExpression(final Expression entityExpression, final OrExpression fieldExpression) {
        this.entityExpression = entityExpression;
        this.fieldExpression = fieldExpression;
    }

    @Override
    public ExpressionResult evaluate() {
        ExpressionResult fieldResult = fieldExpression.evaluate();

        if (fieldResult.getStatus() != FAIL) {
            return fieldResult;
        }

        return (entityExpression == null) ? PASS_RESULT : entityExpression.evaluate();
    }
}
