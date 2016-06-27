/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.EntityDictionary;

import java.util.function.Predicate;

/**
 * Visitor for in memory filterExpressions
 */
public class InMemoryFilterVisitor implements Visitor<Predicate> {
    private final EntityDictionary dictionary;

    public InMemoryFilterVisitor(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public Predicate visitPredicate(com.yahoo.elide.core.filter.Predicate predicate) {
        return predicate.apply(dictionary);
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
