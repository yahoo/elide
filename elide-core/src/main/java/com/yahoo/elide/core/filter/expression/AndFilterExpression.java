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

    /**
     * Returns a new {@link AndFilterExpression} instance with the specified null-able left and right operands.
     * <p>
     * If both left and right are not {@link null}, this method produces the same instance as
     * {@link #AndFilterExpression(FilterExpression, FilterExpression)} does. If only one of them is {@link null}, the
     * other non-null is returned with no modification. If both left and right are {@link null}, this method returns
     * {@code null}.
     *
     * @param left  The provided left {@link FilterExpression}
     * @param right  The provided right {@link FilterExpression}
     *
     * @return a new {@link AndFilterExpression} instance or {@code null}
     */
    public static FilterExpression and(FilterExpression left, FilterExpression right) {
        if (left != null && right != null) {
            return new AndFilterExpression(left, right);
        } else if (left == null) {
            return right;
        }
        return left;
    }

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
