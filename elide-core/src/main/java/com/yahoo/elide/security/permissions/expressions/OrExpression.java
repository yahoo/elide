/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.permissions.ExpressionResult;

import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED_RESULT;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS_RESULT;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.PASS;
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
        if (leftResult.getStatus() == PASS) {
            return PASS_RESULT;
        }

        ExpressionResult rightResult = (right == null) ? leftResult : right.evaluate();

        if (leftResult.getStatus() == FAIL && rightResult.getStatus() == FAIL) {
            return leftResult;
        }

        if (rightResult.getStatus() == PASS) {
            return PASS_RESULT;
        }

        return DEFERRED_RESULT;
    }
}
