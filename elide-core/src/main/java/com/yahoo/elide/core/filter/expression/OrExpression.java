/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.expression;

import lombok.Getter;

/**
 * An 'Or' Filter Expression.
 */
public class OrExpression implements Expression {

    @Getter private Expression left;
    @Getter private Expression right;

    public OrExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;

    }
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitOrExpression(this);
    }

    @Override
    public String toString() {
        return String.format("(%s OR %s)", left, right);
    }
}
