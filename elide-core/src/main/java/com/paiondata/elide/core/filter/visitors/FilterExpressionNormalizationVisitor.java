/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.filter.visitors;

import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpressionVisitor;
import com.paiondata.elide.core.filter.expression.NotFilterExpression;
import com.paiondata.elide.core.filter.expression.OrFilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;


/**
 * Expression Visitor.
 */
public class FilterExpressionNormalizationVisitor implements FilterExpressionVisitor<FilterExpression> {

    @Override
    public FilterExpression visitPredicate(FilterPredicate filterPredicate) {
        return filterPredicate;
    }

    @Override
    public FilterExpression visitAndExpression(AndFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        return new AndFilterExpression(left.accept(this), right.accept(this));
    }

    @Override
    public FilterExpression visitOrExpression(OrFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        return new OrFilterExpression(left.accept(this), right.accept(this));
    }

    @Override
    public FilterExpression visitNotExpression(NotFilterExpression fe) {
        FilterExpression inner = fe.getNegated();
        if (inner instanceof AndFilterExpression) {
            AndFilterExpression and = (AndFilterExpression) inner;
            FilterExpression left = new NotFilterExpression(and.getLeft()).accept(this);
            FilterExpression right = new NotFilterExpression(and.getRight()).accept(this);
            return new OrFilterExpression(left, right);
        }
        if (inner instanceof OrFilterExpression) {
            OrFilterExpression or = (OrFilterExpression) inner;
            FilterExpression left = new NotFilterExpression(or.getLeft()).accept(this);
            FilterExpression right = new NotFilterExpression(or.getRight()).accept(this);
            return new AndFilterExpression(left, right);
        }
        if (inner instanceof NotFilterExpression) {
            NotFilterExpression not = (NotFilterExpression) inner;
            return (not.getNegated()).accept(this);
        }
        if (inner instanceof FilterPredicate) {
            FilterPredicate filter = (FilterPredicate) inner;
            return filter.negate();
        }
        return inner;
    }
}
