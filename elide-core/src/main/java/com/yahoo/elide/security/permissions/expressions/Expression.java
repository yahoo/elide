/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import static org.fusesource.jansi.Ansi.ansi;

import com.yahoo.elide.security.permissions.ExpressionResult;

import org.fusesource.jansi.Ansi;

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

    /**
     * Static Expressions that return PASS or FAIL.
     */
    public static class Results {
        public static final Expression SUCCESS = new Expression() {
            @Override
            public ExpressionResult evaluate(EvaluationMode ignored) {
                return ExpressionResult.PASS;
            }

            @Override
            public String toString() {
                return ansi().fg(Ansi.Color.GREEN).a("SUCCESS").reset().toString();
            }
        };
        public static final Expression FAILURE = new Expression() {
            @Override
            public ExpressionResult evaluate(EvaluationMode ignored) {
                return ExpressionResult.FAIL;
            }

            @Override
            public String toString() {
                return ansi().fg(Ansi.Color.RED).a("FAILURE").reset().toString();
            }
        };
    }
}
