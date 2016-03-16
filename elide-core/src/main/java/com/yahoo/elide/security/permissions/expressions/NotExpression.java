/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;


import com.yahoo.elide.security.permissions.ExpressionResult;

import static com.yahoo.elide.security.permissions.ExpressionResult.PASS_RESULT;
import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED_RESULT;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.PASS;

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
    public ExpressionResult evaluate() {
        ExpressionResult logicalResult = logical.evaluate();

        if (logicalResult.getStatus() == FAIL) {
            return PASS_RESULT;
        }

        if (logicalResult.getStatus() == PASS) {
            return new ExpressionResult(FAIL, logicalResult.getFailureMessage());
        }

        return DEFERRED_RESULT;
    }
}
