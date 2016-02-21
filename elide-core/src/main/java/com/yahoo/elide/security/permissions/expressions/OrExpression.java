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
        ExpressionResult rightResult = (right == null) ? leftResult : right.evaluate();

        if (leftResult == FAIL && rightResult == FAIL) {
            return FAIL;
        }

        if (leftResult == PASS || rightResult == PASS) {
            return PASS;
        }

        return DEFERRED;
    }
}
