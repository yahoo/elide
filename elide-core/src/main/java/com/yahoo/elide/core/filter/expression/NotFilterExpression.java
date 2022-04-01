/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A 'Not' Filter FilterExpression.
 */
@EqualsAndHashCode
public class NotFilterExpression implements FilterExpression {

    @Getter private FilterExpression negated;

    public NotFilterExpression(FilterExpression negated) {
        this.negated = negated;

    }
    @Override
    public <T> T accept(FilterExpressionVisitor<T> visitor) {
        return visitor.visitNotExpression(this);
    }

    @Override
    public String toString() {
        return String.format("NOT (%s)", negated);
    }
}
