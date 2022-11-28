/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.filter.predicates.FilterPredicate;

import lombok.Getter;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * A Visitor which extracts the set of predicates from a filter FilterExpression.
 * Should only be used in Elide 2.0 scope
 */
public class PredicateExtractionVisitor implements FilterExpressionVisitor<Collection<FilterPredicate>> {
    @Getter Collection<FilterPredicate> filterPredicates;

    /**
     * Defaults to extracting a set of predicates.
     */
    public PredicateExtractionVisitor() {
        filterPredicates = new LinkedHashSet<>();
    }

    /**
     * Extracts predicates into the provided collection.
     * @param predicates The collection (list, set, etc) to store the predicates in.
     */
    public PredicateExtractionVisitor(Collection<FilterPredicate> predicates) {
        filterPredicates = predicates;
    }

    @Override
    public Collection<FilterPredicate> visitPredicate(FilterPredicate filterPredicate) {
        filterPredicates.add(filterPredicate);
        return filterPredicates;
    }

    @Override
    public Collection<FilterPredicate> visitAndExpression(AndFilterExpression expression) {
        expression.getLeft().accept(this);
        expression.getRight().accept(this);
        return filterPredicates;
    }

    @Override
    public Collection<FilterPredicate> visitOrExpression(OrFilterExpression expression) {
        expression.getLeft().accept(this);
        expression.getRight().accept(this);
        return filterPredicates;
    }

    @Override
    public Collection<FilterPredicate> visitNotExpression(NotFilterExpression expression) {
        expression.getNegated().accept(this);
        return filterPredicates;
    }
}
