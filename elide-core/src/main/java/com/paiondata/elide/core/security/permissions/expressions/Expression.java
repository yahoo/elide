/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.permissions.expressions;

import com.paiondata.elide.core.security.permissions.ExpressionResult;

/**
 * Interface describing an expression.
 */
public interface Expression {

    /**
     * Different modes of evaluating security expressions.
     */
    public enum EvaluationMode {
        USER_CHECKS_ONLY,   /* Only the user checks are evaluated */
        INLINE_CHECKS_ONLY, /* Only the inline checks are evaluated */
        ALL_CHECKS          /* Everything is evaluated */
    }

    /**
     * Evaluate an expression.
     *
     * @param mode mode for evaluating security expressions
     * @return The result of the fully evaluated expression.
     */
    ExpressionResult evaluate(EvaluationMode mode);

    public <T> T accept(ExpressionVisitor<T> visitor);

    /**
     * Static Expressions that return PASS or FAIL.
     */
    public static class Results {
        public static final Expression SUCCESS = new BooleanExpression(true);
        public static final Expression FAILURE = new BooleanExpression(false);
    }
}
