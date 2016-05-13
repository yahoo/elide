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
                return ExpressionResult.PASS;
            }

            @Override
            public String toString() {
                return ansi().fg(Ansi.Color.GREEN).a("SUCCESS").reset().toString();
            }
        };
        public static final Expression FAILURE = new Expression() {
            @Override
            public ExpressionResult evaluate() {
                return ExpressionResult.FAIL;
            }

            @Override
            public String toString() {
                return ansi().fg(Ansi.Color.RED).a("FAILURE").reset().toString();
            }
        };
    }
}
