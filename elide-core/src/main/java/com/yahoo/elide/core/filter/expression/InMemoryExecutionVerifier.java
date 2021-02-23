/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.type.Type;

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
        for (Path.PathElement pathElement : filterPredicate.getPath().getPathElements()) {
            Type<?> entityClass = pathElement.getType();
            String fieldName = pathElement.getFieldName();

            if (dictionary.isComputed(entityClass, fieldName)) {
                return true;
            }
        }
        return false;
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
     * @param dictionary
     * @param expression
     * @return Returns true if the filter expression must be evaluated in memory.
     */
    public static boolean shouldExecuteInMemory(EntityDictionary dictionary, FilterExpression expression) {
        InMemoryExecutionVerifier verifier = new InMemoryExecutionVerifier(dictionary);

        return expression.accept(verifier);
    }
}
