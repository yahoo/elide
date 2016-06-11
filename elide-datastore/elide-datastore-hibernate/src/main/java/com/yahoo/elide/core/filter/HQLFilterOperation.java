/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.expression.AndExpression;
import com.yahoo.elide.core.filter.expression.Expression;
import com.yahoo.elide.core.filter.expression.NotExpression;
import com.yahoo.elide.core.filter.expression.OrExpression;
import com.yahoo.elide.core.filter.expression.Visitor;

import java.util.Set;

/**
 * FilterOperation that creates Hibernate query language fragments.
 */
public class HQLFilterOperation implements FilterOperation<String> {
    @Override
    public String apply(Predicate predicate) {
        switch (predicate.getOperator()) {
            case IN:
                return String.format("%s IN (:%s)", predicate.getField(), predicate.getField());
            case NOT:
                return String.format("%s NOT IN (:%s)", predicate.getField(), predicate.getField());
            case PREFIX:
                return String.format("%s LIKE CONCAT(:%s, '%%')", predicate.getField(), predicate.getField());
            case POSTFIX:
                return String.format("%s LIKE CONCAT('%%', :%s)", predicate.getField(), predicate.getField());
            case INFIX:
                return String.format("%s LIKE CONCAT('%%', :%s, '%%')", predicate.getField(), predicate.getField());
            case ISNULL:
                return String.format("%s IS NULL", predicate.getField());
            case NOTNULL:
                return String.format("%s IS NOT NULL", predicate.getField());
            case LT:
                return String.format("%s < :%s", predicate.getField(), predicate.getField());
            case LE:
                return String.format("%s <= :%s", predicate.getField(), predicate.getField());
            case GT:
                return String.format("%s > :%s", predicate.getField(), predicate.getField());
            case GE:
                return String.format("%s >= :%s", predicate.getField(), predicate.getField());

            default:
                throw new InvalidPredicateException("Operator not implemented: " + predicate.getOperator());
        }
    }

    @Override
    public String applyAll(Set<Predicate> predicates) {
        StringBuilder filterString = new StringBuilder();

        for (Predicate predicate : predicates) {
            if (filterString.length() == 0) {
                filterString.append("WHERE ");
            } else {
                filterString.append(" AND ");
            }

            filterString.append(apply(predicate));
        }

        return filterString.toString();
    }

    public String apply(Expression filterExpression) {
        HQLQueryVisitor visitor = new HQLQueryVisitor();
        return "WHERE " + filterExpression.accept(visitor);

    }

    /**
     * Filter expression visitor which builds an HQL query.
     */
    public class HQLQueryVisitor implements Visitor<String> {

        private String query;

        @Override
        public String visitPredicate(Predicate predicate) {
            query = apply(predicate);
            return query;
        }

        @Override
        public String visitAndExpression(AndExpression expression) {
            String left = expression.getLeft().accept(this);
            String right = expression.getRight().accept(this);
            query = "(" + left + " AND " + right + ")";
            return query;
        }

        @Override
        public String visitOrExpression(OrExpression expression) {
            String left = expression.getLeft().accept(this);
            String right = expression.getRight().accept(this);
            query = "(" + left + " OR " + right + ")";
            return query;
        }

        @Override
        public String visitNotExpression(NotExpression expression) {
            String negated = expression.getNegated().accept(this);
            query = "NOT (" + negated + ")";
            return query;
        }
    }
}
