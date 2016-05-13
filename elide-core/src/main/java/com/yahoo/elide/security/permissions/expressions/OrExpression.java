/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.permissions.ExpressionResult;

import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED;
import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS;

/**
 * Representation of an "or" expression.
 */
public class OrExpression implements Expression {
    private final Expression left;
    private final Expression right;

    public static final OrExpression SUCCESSFUL_EXPRESSION = new OrExpression(Results.SUCCESS, null);

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
    public ExpressionResult evaluate() {
        ExpressionResult leftResult = left.evaluate();

        // Short-circuit
        if (leftResult == PASS) {
            return PASS;
        }

        ExpressionResult rightResult = (right == null) ? leftResult : right.evaluate();

        if (leftResult == FAIL && rightResult == FAIL) {
            return leftResult;
        }

        if (rightResult == PASS) {
            return PASS;
        }

        return DEFERRED;
    }

    @Override
    public String toString() {
        if (right == null || right.equals(Results.FAILURE)) {
            return String.format("%s", left);
        }
        return String.format("(%s) OR (%s)", left, right);
    }
}
