/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;


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
        return new AndFilterExpression(expression.getLeft().accept(this), expression.getRight().accept(this));
    }

    @Override
    public FilterExpression visitOrExpression(OrFilterExpression expression) {
        return new OrFilterExpression(expression.getLeft().accept(this), expression.getRight().accept(this));
    }

    @Override
    public FilterExpression visitNotExpression(NotFilterExpression fe) {
        FilterExpression nfe = fe.getNegated();
        if (nfe instanceof AndFilterExpression) {
            AndFilterExpression and = (AndFilterExpression) nfe;
            FilterExpression left = (new NotFilterExpression(and.getLeft())).accept(this);
            FilterExpression right = (new NotFilterExpression(and.getRight())).accept(this);
            return new OrFilterExpression(left, right);
        }
        if (nfe instanceof OrFilterExpression) {
            OrFilterExpression or = (OrFilterExpression) nfe;
            FilterExpression left = (new NotFilterExpression(or.getLeft())).accept(this);
            FilterExpression right = (new NotFilterExpression(or.getRight())).accept(this);
            return new AndFilterExpression(left, right);
        }
        if (nfe instanceof FilterPredicate) {
            ((FilterPredicate) nfe).negate();
            return nfe;
        }
        if (nfe instanceof NotFilterExpression) {
            return (((NotFilterExpression) nfe).getNegated()).accept(this);
        }
        return nfe;
    }
}
