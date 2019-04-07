/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.parsers.expression.FilterExpressionNormalizationVisitor;

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

        boolean filterInMemory = false;
        for (Path.PathElement pathElement : filterPredicate.getPath().getPathElements()) {
            Class<?> entityClass = pathElement.getType();
            String fieldName = pathElement.getFieldName();

            if (dictionary.isComputed(entityClass, fieldName)) {
                filterInMemory = true;
            }
        }

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

        return expression;
    }

    @Override
    public FilterExpression visitOrExpression(OrFilterExpression expression) {
        FilterExpression left = expression.getLeft().accept(this);
        FilterExpression right = expression.getRight().accept(this);

        if (left == null || right == null) {
            return null;
        }
        return expression;
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
     * @param dictionary
     * @param expression
     * @return A filter expression that can be safely executed in the data store.
     */
    public static FilterExpression extractPushDownPredicate(EntityDictionary dictionary, FilterExpression expression) {
        FilterExpressionNormalizationVisitor normalizationVisitor = new FilterExpressionNormalizationVisitor();
        FilterExpression normalizedExpression = expression.accept(normalizationVisitor);
        FilterPredicatePushdownExtractor verifier = new FilterPredicatePushdownExtractor(dictionary);

        return normalizedExpression.accept(verifier);
    }
}
