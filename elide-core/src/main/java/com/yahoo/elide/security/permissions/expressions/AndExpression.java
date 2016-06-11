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
        ExpressionResult leftStatus = left.evaluate();

        // Short-circuit
        if (leftStatus == FAIL) {
            return leftStatus;
        }

        ExpressionResult rightStatus = (right == null) ? PASS : right.evaluate();

        if (rightStatus == FAIL) {
            return rightStatus;
        }

        if (leftStatus == PASS && rightStatus == PASS) {
            return PASS;
        }

        return DEFERRED;
    }

    @Override
    public String toString() {
        if (right == null) {
            return String.format("%s", left);

        }
        return String.format("(%s) AND (%s)", left, right);
    }
}
