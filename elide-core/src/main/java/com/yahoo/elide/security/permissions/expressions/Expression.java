/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.permissions.ExpressionResult;

/**
 * Interface describing an expression.
 */
public interface Expression {
    /**
     * Evaluate an expression.
     *
     * @return The result of the fully evaluated expression.
     */
    ExpressionResult evaluate();

    /**
     * Static Expressions that return PASS or FAIL.
     */
    public static class Results {
        public static final Expression SUCCESS = new Expression() {
            @Override
            public ExpressionResult evaluate() {
                return ExpressionResult.PASS_RESULT;
            }

            @Override
            public String toString() {
                return "SUCCESS";
            }
        };
        public static final Expression FAILURE = new Expression() {
            @Override
            public ExpressionResult evaluate() {
                return new ExpressionResult(ExpressionResult.Status.FAIL);
            }

            @Override
            public String toString() {
                return "FAILURE";
            }
        };
    }
}
