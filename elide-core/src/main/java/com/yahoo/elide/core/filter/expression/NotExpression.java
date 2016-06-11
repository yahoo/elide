/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import lombok.Getter;

/**
 * A 'Not' Filter Expression.
 */
public class NotExpression implements Expression {

    @Getter private Expression negated;

    public NotExpression(Expression negated) {
        this.negated = negated;

    }
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitNotExpression(this);
    }

    @Override
    public String toString() {
        return String.format("NOT (%s)", negated);
    }
}
