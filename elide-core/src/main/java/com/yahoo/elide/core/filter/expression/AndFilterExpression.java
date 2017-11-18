/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * An 'And' Filter FilterExpression.
 */
@EqualsAndHashCode
public class AndFilterExpression implements FilterExpression {

    @Getter private FilterExpression left;
    @Getter private FilterExpression right;

    public AndFilterExpression(FilterExpression left, FilterExpression right) {
        this.left = left;
        this.right = right;

    }
    @Override
    public <T> T accept(FilterExpressionVisitor<T> visitor) {
        return visitor.visitAndExpression(this);
    }

    @Override
    public String toString() {
        return String.format("(%s AND %s)", left, right);
    }
}
