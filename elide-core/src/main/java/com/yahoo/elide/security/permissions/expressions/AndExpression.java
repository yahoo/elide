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
 * Representation for an "And" expression.
 */
public class AndExpression implements Expression {
    private final Expression left;
    private final Expression right;

    /**
     * Constructor.
     *
     * @param left Left expression
     * @param right Right expression
     */
    public AndExpression(final Expression left, final Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public ExpressionResult evaluate() {
        final ExpressionResult leftResult = left.evaluate();

        // Short-circuit
        if (leftResult.getStatus() == FAIL) {
            return leftResult;
        }

        final ExpressionResult rightResult = (right == null) ? PASS_RESULT : right.evaluate();

        if (rightResult.getStatus() == FAIL) {
            return rightResult;
        }

        if (leftResult.getStatus() == PASS && rightResult.getStatus() == PASS) {
            return PASS_RESULT;
        }

        return DEFERRED_RESULT;
    }
}
