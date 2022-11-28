/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.permissions.expressions;

import static com.yahoo.elide.core.security.permissions.ExpressionResult.DEFERRED;
import static com.yahoo.elide.core.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.core.security.permissions.ExpressionResult.PASS;

import com.yahoo.elide.core.security.permissions.ExpressionResult;

import lombok.Getter;

/**
 * Representation of an "or" expression.
 */
public class OrExpression implements Expression {
    @Getter
    private final Expression left;
    @Getter
    private final Expression right;

    public static final OrExpression SUCCESSFUL_EXPRESSION = new OrExpression(Results.SUCCESS, null);
    public static final OrExpression FAILURE_EXPRESSION = new OrExpression(Results.FAILURE, null);

    /**
     * Constructor.
     *
     * @param left Left expression
     * @param right Right expression.
     */
    public OrExpression(final Expression left, final Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        ExpressionResult leftResult = left.evaluate(mode);

        // Short-circuit
        if (leftResult == PASS) {
            return PASS;
        }

        ExpressionResult rightResult = (right == null) ? leftResult : right.evaluate(mode);

        if (leftResult == FAIL && rightResult == FAIL) {
            return leftResult;
        }

        if (rightResult == PASS) {
            return PASS;
        }

        return DEFERRED;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitOrExpression(this);
    }

    @Override
    public String toString() {
        if (right == null || right.equals(Results.FAILURE)) {
            return String.format("%s", left);
        }
        return String.format("(%s) OR (%s)", left, right);
    }
}
