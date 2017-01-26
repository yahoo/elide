/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.filter.FilterPredicate;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * A Visitor which extracts the set of predicates from a filter FilterExpression.
 * Should only be used in Elide 2.0 scope
 */
public class PredicateExtractionVisitor implements Visitor<Set<FilterPredicate>> {
    @Getter Set<FilterPredicate> filterPredicates = new HashSet<>();

    @Override
    public Set<FilterPredicate> visitPredicate(FilterPredicate filterPredicate) {
        filterPredicates.add(filterPredicate);
        return filterPredicates;
    }

    @Override
    public Set<FilterPredicate> visitAndExpression(AndFilterExpression expression) {
        filterPredicates.addAll(expression.getLeft().accept(this));
        filterPredicates.addAll(expression.getRight().accept(this));
        return filterPredicates;
    }

    @Override
    public Set<FilterPredicate> visitOrExpression(OrFilterExpression expression) {
        filterPredicates.addAll(expression.getLeft().accept(this));
        filterPredicates.addAll(expression.getRight().accept(this));
        return filterPredicates;
    }

    @Override
    public Set<FilterPredicate> visitNotExpression(NotFilterExpression expression) {
        filterPredicates.addAll(expression.getNegated().accept(this));
        return filterPredicates;
    }
}
