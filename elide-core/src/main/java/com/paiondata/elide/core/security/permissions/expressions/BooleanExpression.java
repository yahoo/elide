/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.security.permissions.expressions;

import static org.fusesource.jansi.Ansi.ansi;

import com.paiondata.elide.core.security.permissions.ExpressionResult;
import org.fusesource.jansi.Ansi;

/**
 * Expression that returns always true (PASS) or false (FAILURE).
 */
public class BooleanExpression implements Expression {
    private boolean value;
    public BooleanExpression(boolean value) {
        this.value = value;
    }
    @Override
    public ExpressionResult evaluate(EvaluationMode mode) {
        if (value == true) {
            return ExpressionResult.PASS;
        }
        return ExpressionResult.FAIL;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitBooleanExpression(this);
    }

    @Override
    public String toString() {
        Ansi.Color color = value ? Ansi.Color.GREEN : Ansi.Color.RED;
        String label = value ? "SUCCESS" : "FAILURE";
        return ansi().fg(color).a(label).reset().toString();
    }
}
