/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.permissions.ExpressionResult;

import java.util.Optional;

import static com.yahoo.elide.security.permissions.ExpressionResult.PASS_RESULT;

/**
 * Expression for joining specific fields.
 *
 * That is, this evaluates security while giving precedence to the annotation on a particular field over
 * the annotation at the entity- or package-level.
 */
public class SpecificFieldExpression implements Expression {
    private final Expression entityExpression;
    private final Optional<Expression> fieldExpression;

    public SpecificFieldExpression(final Expression entityExpression, final Expression fieldExpression) {
        this.entityExpression = entityExpression;
        this.fieldExpression = Optional.ofNullable(fieldExpression);
    }

    @Override
    public ExpressionResult evaluate() {
        if (!fieldExpression.isPresent()) {
            return (entityExpression == null) ? PASS_RESULT : entityExpression.evaluate();
        }
        return fieldExpression.get().evaluate();
    }
}
