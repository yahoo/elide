/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * An 'Or' Filter FilterExpression.
 */
@EqualsAndHashCode
public class OrFilterExpression implements FilterExpression {

    @Getter private FilterExpression left;
    @Getter private FilterExpression right;

    /**
     * Returns a new {@link OrFilterExpression} instance with the specified null-able left and right operands.
     * <p>
     * The publication rules are
     * <ol>
     *     <li> If both left and right are not {@code null}, this method produces the same instance as
     *          {@link #OrFilterExpression(FilterExpression, FilterExpression)} does,
     *     <li> If one of them is {@code null}, the other non-null is returned with no modification,
     *     <li> If both left and right are {@code null}, this method returns
     *          {@code null}.
     * </ol>
     *
     * @param left  The provided left {@link FilterExpression}
     * @param right  The provided right {@link FilterExpression}
     *
     * @return a new {@link OrFilterExpression} instance or {@code null}
     */
    public static FilterExpression fromPair(FilterExpression left, FilterExpression right) {
        if (left != null && right != null) {
            return new OrFilterExpression(left, right);
        } else if (left == null) {
            return right;
        }
        return left;
    }

    public OrFilterExpression(FilterExpression left, FilterExpression right) {
        this.left = left;
        this.right = right;

    }
    @Override
    public <T> T accept(FilterExpressionVisitor<T> visitor) {
        return visitor.visitOrExpression(this);
    }

    @Override
    public String toString() {
        return String.format("(%s OR %s)", left, right);
    }
}
