/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.filter.FilterPredicate;

/**
 * Visitor which walks the filter expression abstract syntax tree.
 * @param <T> The return type of the visitor
 */
public interface FilterExpressionVisitor<T> {
    T visitPredicate(FilterPredicate filterPredicate);
    T visitAndExpression(AndFilterExpression expression);
    T visitOrExpression(OrFilterExpression expression);
    T visitNotExpression(NotFilterExpression expression);
}
