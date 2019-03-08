/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.FilterPredicate;

import java.util.function.Predicate;

/**
 * Visitor for in memory filterExpressions.
 */
public class InMemoryFilterExecutor implements FilterExpressionVisitor<Predicate> {
    private final RequestScope requestScope;

    public InMemoryFilterExecutor(RequestScope requestScope) {
        this.requestScope = requestScope;
    }

    @Override
    public Predicate visitPredicate(FilterPredicate filterPredicate) {
        return filterPredicate.apply(requestScope);
    }

    @Override
    public Predicate visitAndExpression(AndFilterExpression expression) {
        Predicate leftPredicate = expression.getLeft().accept(this);
        Predicate rightPredicate = expression.getRight().accept(this);
        return t -> leftPredicate.and(rightPredicate).test(t);
    }

    @Override
    public Predicate visitOrExpression(OrFilterExpression expression) {
        Predicate leftPredicate = expression.getLeft().accept(this);
        Predicate rightPredicate = expression.getRight().accept(this);
        return t -> leftPredicate.or(rightPredicate).test(t);
    }

    @Override
    public Predicate visitNotExpression(NotFilterExpression expression) {
        Predicate predicate = expression.getNegated().accept(this);
        return t -> predicate.negate().test(t);
    }
}
