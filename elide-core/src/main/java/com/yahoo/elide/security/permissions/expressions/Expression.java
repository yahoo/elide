/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.permissions.ExpressionResult;
import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Interface describing an expression.
 */
public interface Expression {

    public enum EvaluationMode {
        USER_CHECKS_ONLY,
        INLINE_CHECKS_ONLY,
        ALL_CHECKS
    }

    /**
     * Evaluate an expression.
     *
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
