/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED;
import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS;

import com.yahoo.elide.security.permissions.ExpressionResult;

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
    public ExpressionResult evaluate(EvaluationMode mode) {
        ExpressionResult leftStatus = left.evaluate(mode);

        // Short-circuit
        if (leftStatus == FAIL) {
            return leftStatus;
        }

        ExpressionResult rightStatus = (right == null) ? PASS : right.evaluate(mode);

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
