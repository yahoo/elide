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
 * Representation for an "And" expression.
 */
public class AndExpression implements Expression {
    @Getter
    private final Expression left;
    @Getter
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
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitAndExpression(this);
    }

    @Override
    public String toString() {
        if (right == null) {
            return String.format("%s", left);

        }
        return String.format("(%s) AND (%s)", left, right);
    }
}
