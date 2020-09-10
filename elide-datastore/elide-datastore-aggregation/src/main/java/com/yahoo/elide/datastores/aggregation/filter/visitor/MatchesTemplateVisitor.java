/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.filter.visitor;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.parsers.expression.FilterExpressionNormalizationVisitor;

import com.google.common.base.Preconditions;

import lombok.AllArgsConstructor;

/**
 * Validates whether a client provided filter either:
 * 1. Directly matches a template filter.
 * 2. Contains through conjunction (logical AND) a filter expression that matches a template filter.
 * This is used to enforce table filter constraints.
 */
@AllArgsConstructor
public class MatchesTemplateVisitor implements FilterExpressionVisitor<Boolean> {
    private FilterExpression expressionToMatch;

    @Override
    public Boolean visitPredicate(FilterPredicate filterPredicate) {
        if (matches(expressionToMatch, filterPredicate)) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean visitAndExpression(AndFilterExpression expression) {
        if (matches(expressionToMatch, expression)) {
            return true;
        }
        return expression.getLeft().accept(this) || expression.getRight().accept(this);
    }

    @Override
    public Boolean visitOrExpression(OrFilterExpression expression) {
        if (matches(expressionToMatch, expression)) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean visitNotExpression(NotFilterExpression expression) {
        if (matches(expressionToMatch, expression)) {
            return true;
        }
        return false;
    }

    private static boolean matches(FilterExpression a, FilterExpression b) {
        if (! a.getClass().equals(b.getClass())) {
            return false;
        }

        if (a instanceof AndFilterExpression) {
            AndFilterExpression andA = (AndFilterExpression) a;
            AndFilterExpression andB = (AndFilterExpression) b;

            return matches(andA.getLeft(), andB.getLeft()) && matches(andA.getRight(), andB.getRight());
        } else if (a instanceof OrFilterExpression) {
            OrFilterExpression orA = (OrFilterExpression) a;
            OrFilterExpression orB = (OrFilterExpression) b;

            return matches(orA.getLeft(), orB.getLeft()) && matches(orA.getRight(), orB.getRight());
        } else if (a instanceof NotFilterExpression) {
            NotFilterExpression notA = (NotFilterExpression) a;
            NotFilterExpression notB = (NotFilterExpression) b;

            return matches(notA.getNegated(), notB.getNegated());
        } else {
            FilterPredicate predicateA = (FilterPredicate) a;
            FilterPredicate predicateB = (FilterPredicate) b;

            return predicateA.getPath().equals(predicateB.getPath())
                    && predicateA.getOperator().equals(predicateB.getOperator());
        }
    }

    /**
     * Determines if a client filter matches or contains a subexpression that matches a template filter.
     * @param templateFilter A templated filter expression
     * @param clientFilter The client provided filter expression.
     * @return True if the client filter matches.  False otherwise.
     */
    public static boolean isValid(FilterExpression templateFilter, FilterExpression clientFilter) {
        Preconditions.checkNotNull(templateFilter);

        if (clientFilter == null) {
            return false;
        }

        //First we normalize the filters so any NOT clauses are pushed down immediately in front of a predicate.
        //This lets us treat logical AND and OR without regard for any preceding NOT clauses.
        FilterExpression normalizedTemplateFilter = templateFilter.accept(new FilterExpressionNormalizationVisitor());
        FilterExpression normalizedClientFilter = clientFilter.accept(new FilterExpressionNormalizationVisitor());

        return normalizedClientFilter.accept(new MatchesTemplateVisitor(normalizedTemplateFilter));
    }
}
