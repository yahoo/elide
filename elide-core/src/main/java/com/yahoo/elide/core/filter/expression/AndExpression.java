/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import lombok.Getter;

/**
 * An 'And' Filter Expression.
 */
public class AndExpression implements Expression {

    @Getter private Expression left;
    @Getter private Expression right;

    public AndExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;

    }
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitAndExpression(this);
    }

    @Override
    public String toString() {
        return String.format("(%s AND %s)", left, right);
    }
}
