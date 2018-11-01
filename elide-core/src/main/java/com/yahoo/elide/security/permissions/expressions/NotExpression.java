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
 * Representation of a "not" expression.
 */
public class NotExpression implements Expression {
    private final Expression logical;

    /**
     * Constructor.
     *
     * @param logical Unary expression
     */
    public NotExpression(final Expression logical) {
        this.logical = logical;
    }


    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        ExpressionResult result = logical.evaluate(mode);

        if (result == FAIL) {
            return PASS;
        }

        if (result == PASS) {
            return FAIL;
        }

        return DEFERRED;
    }

    @Override
    public String toString() {
        return String.format("NOT (%s)", logical);
    }
}
