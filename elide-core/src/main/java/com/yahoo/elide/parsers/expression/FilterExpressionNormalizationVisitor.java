/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.expression.Visitor;


/**
 * Expression Visitor.
 */
public class FilterExpressionNormalizationVisitor implements Visitor<FilterExpression> {

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
            FilterExpression left = (new NotFilterExpression(((AndFilterExpression) nfe).getLeft())).accept(this);
            FilterExpression right = (new NotFilterExpression(((AndFilterExpression) nfe).getRight())).accept(this);
            return new OrFilterExpression(left, right);
        } else if (nfe instanceof OrFilterExpression) {
            FilterExpression left = (new NotFilterExpression(((OrFilterExpression) nfe).getLeft())).accept(this);
            FilterExpression right = (new NotFilterExpression(((OrFilterExpression) nfe).getRight())).accept(this);
            return new AndFilterExpression(left, right);
        } else if (nfe instanceof FilterPredicate) {
            ((FilterPredicate) nfe).negate();
            return nfe;
        } else if (nfe instanceof NotFilterExpression) {
            return (((NotFilterExpression) nfe).getNegated()).accept(this);
        } else {
            return nfe;
        }
    }
}
