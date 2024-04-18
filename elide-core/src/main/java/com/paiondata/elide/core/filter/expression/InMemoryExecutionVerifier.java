/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.filter.expression;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;

/**
 * Intended to specify whether the expression must be evaluated in memory or can be pushed to the DataStore.
 * Constructs true if any part of the expression relies on a computed attribute or relationship.
 * Otherwise constructs false.
 */
public class InMemoryExecutionVerifier implements FilterExpressionVisitor<Boolean> {
    private EntityDictionary dictionary;

    public InMemoryExecutionVerifier(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public Boolean visitPredicate(FilterPredicate filterPredicate) {
        return filterPredicate.getPath().isComputed(dictionary);
    }

    @Override
    public Boolean visitAndExpression(AndFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        // is either computed?
        return (left.accept(this) || right.accept(this));
    }

    @Override
    public Boolean visitOrExpression(OrFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        // is either computed?
        return (left.accept(this) || right.accept(this));
    }

    @Override
    public Boolean visitNotExpression(NotFilterExpression expression) {
        return expression.getNegated().accept(this);
    }

    /**
     * Determines if the filter expression should be evaluated in memory.
     *
     * @param dictionary the entity dictionary
     * @param expression the filter expression
     * @return Returns true if the filter expression must be evaluated in memory.
     */
    public static boolean shouldExecuteInMemory(EntityDictionary dictionary, FilterExpression expression) {
        InMemoryExecutionVerifier verifier = new InMemoryExecutionVerifier(dictionary);

        return expression.accept(verifier);
    }
}
