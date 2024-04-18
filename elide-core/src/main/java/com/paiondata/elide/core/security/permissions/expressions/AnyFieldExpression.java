/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.permissions.expressions;

import com.paiondata.elide.core.security.permissions.ExpressionResult;
import com.paiondata.elide.core.security.permissions.PermissionCondition;

/**
 * This check determines if an entity is accessible to the current user.
 *
 * An entity is considered to be accessible if there exists an annotation at _any_ level of the object that
 * Grants access. This means that if access is permitted to any field of the object then the object
 * is accessible, regardless of what any class or package level permissions would permit.
 */
public class AnyFieldExpression implements Expression {
    private final Expression expression;
    private final PermissionCondition condition;

    public AnyFieldExpression(final PermissionCondition condition,
                              final Expression expression) {
        this.condition = condition;
        this.expression = expression;
    }

    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        return expression.evaluate(mode);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitAnyFieldExpression(this);
    }

    @Override
    public String toString() {
        return String.format("%s FOR EXPRESSION [%s]",
                condition, expression);
    }
}
