/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.filter.expression;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.filter.visitors.FilterExpressionNormalizationVisitor;

/**
 * Examines a FilterExpression to determine if some or all of it can be pushed to the data store.
 */
public class FilterPredicatePushdownExtractor implements FilterExpressionVisitor<FilterExpression> {

    private EntityDictionary dictionary;

    public FilterPredicatePushdownExtractor(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public FilterExpression visitPredicate(FilterPredicate filterPredicate) {

        boolean filterInMemory = filterPredicate.getPath().isComputed(dictionary);
        return (filterInMemory) ? null : filterPredicate;
    }

    @Override
    public FilterExpression visitAndExpression(AndFilterExpression expression) {
        FilterExpression left = expression.getLeft().accept(this);
        FilterExpression right = expression.getRight().accept(this);

        if (left == null) {
            return right;
        }

        if (right == null) {
            return left;
        }

        return new AndFilterExpression(left, right);
    }

    @Override
    public FilterExpression visitOrExpression(OrFilterExpression expression) {
        FilterExpression left = expression.getLeft().accept(this);
        FilterExpression right = expression.getRight().accept(this);

        if (left == null || right == null) {
            return null;
        }
        return new OrFilterExpression(left, right);
    }

    @Override
    public FilterExpression visitNotExpression(NotFilterExpression expression) {
        FilterExpression inner = expression.getNegated().accept(this);

        if (inner == null) {
            return null;
        }

        return expression;
    }

    /**
     * Extracts the push down predicate.
     *
     * @param dictionary the entity dictionary
     * @param expression the filter expression
     * @return A filter expression that can be safely executed in the data store.
     */
    public static FilterExpression extractPushDownPredicate(EntityDictionary dictionary, FilterExpression expression) {
        FilterExpressionNormalizationVisitor normalizationVisitor = new FilterExpressionNormalizationVisitor();
        FilterExpression normalizedExpression = expression.accept(normalizationVisitor);
        FilterPredicatePushdownExtractor verifier = new FilterPredicatePushdownExtractor(dictionary);

        return normalizedExpression.accept(verifier);
    }
}
