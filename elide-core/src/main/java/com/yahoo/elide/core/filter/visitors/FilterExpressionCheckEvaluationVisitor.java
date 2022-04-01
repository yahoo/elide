/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.visitors;

import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;

/**
 * FilterExpressionCheckEvaluationVisitor evaluate a check against fields of a returning object from datastore.
 */

public class FilterExpressionCheckEvaluationVisitor implements FilterExpressionVisitor<Boolean> {
    private final Object object;
    private final FilterExpressionCheck filterExpressionCheck;
    private final RequestScope requestScope;
    public FilterExpressionCheckEvaluationVisitor(Object object,
                                                  FilterExpressionCheck filterExpressionCheck,
                                                  RequestScope requestScope) {
        this.object = object;
        this.filterExpressionCheck = filterExpressionCheck;
        this.requestScope = requestScope;
    }

    @Override
    public Boolean visitPredicate(FilterPredicate filterPredicate) {
        return filterExpressionCheck.applyPredicateToObject(object, filterPredicate, requestScope);
    }

    @Override
    public Boolean visitAndExpression(AndFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        return left.accept(this) && right.accept(this);
    }

    @Override
    public Boolean visitOrExpression(OrFilterExpression expression) {
        FilterExpression left = expression.getLeft();
        FilterExpression right = expression.getRight();
        return left.accept(this) || right.accept(this);
    }

    @Override
    public Boolean visitNotExpression(NotFilterExpression expression) {
        FilterExpression negation = expression.getNegated();
        return !negation.accept(this);
    }
}
