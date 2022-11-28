/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.permissions.expressions;

import static com.yahoo.elide.core.security.permissions.ExpressionResult.PASS;

import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.security.permissions.PermissionCondition;

import lombok.Getter;

import java.util.Optional;

/**
 * Expression for joining specific fields.
 *
 * That is, this evaluates security while giving precedence to the annotation on a particular field over
 * the annotation at the entity- or package-level.
 */
public class SpecificFieldExpression implements Expression {
    private final Expression entityExpression;
    private final Optional<Expression> fieldExpression;
    @Getter private final PermissionCondition condition;

    public SpecificFieldExpression(final PermissionCondition condition,
                                   final Expression entityExpression,
                                   final Expression fieldExpression) {
        this.condition = condition;
        this.entityExpression = entityExpression;
        this.fieldExpression = Optional.ofNullable(fieldExpression);
    }

    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        if (!fieldExpression.isPresent()) {
            return (entityExpression == null) ? PASS : entityExpression.evaluate(mode);
        }
        return fieldExpression.get().evaluate(mode);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitSpecificFieldExpression(this);
    }


    @Override
    public String toString() {
        return fieldExpression
                .map(fe -> String.format("%s FOR EXPRESSION [FIELD(%s)]", condition, fe))
                .orElseGet(() -> {
                    if (entityExpression == null) {
                        return String.format("%s FOR EXPRESSION []", condition);
                    }
                    return String.format("%s FOR EXPRESSION [ENTITY(%s)]", condition, entityExpression);
                });
    }
}
