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
public interface Visitor<T> {
    public T visitPredicate(FilterPredicate filterPredicate);
    public T visitAndExpression(AndFilterExpression expression);
    public T visitOrExpression(OrFilterExpression expression);
    public T visitNotExpression(NotFilterExpression expression);
}
