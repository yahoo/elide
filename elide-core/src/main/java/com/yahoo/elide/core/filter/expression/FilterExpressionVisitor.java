/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

/**
 * Visitor for filterExpression .
 * @param <T> The return type of the visitor
 */
public interface FilterExpressionVisitor<T> extends Visitor<T> {
    public T visitCustomizedExpression(FilterExpression FilterExpression);
}
