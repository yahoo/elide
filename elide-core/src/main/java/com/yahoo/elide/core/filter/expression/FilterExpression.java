/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

/**
 * A filter expression.
 */
public interface FilterExpression {
    public <T> T accept(FilterExpressionVisitor<T> visitor);
}
