/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.filter.Predicate;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * A Visitor which extracts the set of predicates from a filter FilterExpression.
 * Should only be used in Elide 2.0 scope
 */
public class PredicateExtractionVisitor implements Visitor<Set<Predicate>> {
    @Getter Set<Predicate> predicates = new HashSet<>();

    @Override
    public Set<Predicate> visitPredicate(Predicate predicate) {
        predicates.add(predicate);
        return predicates;
    }

    @Override
    public Set<Predicate> visitAndExpression(AndFilterExpression expression) {
        predicates.addAll(expression.getLeft().accept(this));
        predicates.addAll(expression.getRight().accept(this));
        return predicates;
    }

    @Override
    public Set<Predicate> visitOrExpression(OrFilterExpression expression) {
        predicates.addAll(expression.getLeft().accept(this));
        predicates.addAll(expression.getRight().accept(this));
        return predicates;
    }

    @Override
    public Set<Predicate> visitNotExpression(NotFilterExpression expression) {
        predicates.addAll(expression.getNegated().accept(this));
        return predicates;
    }
}
