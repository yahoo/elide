/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.filter.FilterPredicate;

/**
 * A Visitor which deep clones an entire filter expression.
 */
public class ExpressionCloneVisitor implements Visitor<FilterExpression> {

    @Override
    public FilterExpression visitPredicate(FilterPredicate filterPredicate) {
        return new FilterPredicate(filterPredicate);
    }

    @Override
    public FilterExpression visitAndExpression(AndFilterExpression expression) {
        return new AndFilterExpression(
                expression.getLeft().accept(this),
                expression.getRight().accept(this));
    }

    @Override
    public FilterExpression visitOrExpression(OrFilterExpression expression) {
        return new OrFilterExpression(
                expression.getLeft().accept(this),
                expression.getRight().accept(this));
    }

    @Override
    public FilterExpression visitNotExpression(NotFilterExpression expression) {
        return new NotFilterExpression(expression.getNegated().accept(this));
    }
}
