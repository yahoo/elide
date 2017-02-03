/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.google.common.base.Preconditions;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.expression.Visitor;

import java.util.Set;

/**
 * FilterOperation that creates Hibernate query language fragments.
 */
public class HQLFilterOperation implements FilterOperation<String> {
    @Override
    public String apply(FilterPredicate filterPredicate) {
        String fieldPath = filterPredicate.getFieldPath();
        String alias = filterPredicate.getParameterName();
        switch (filterPredicate.getOperator()) {
            case IN:
                Preconditions.checkState(!filterPredicate.getValues().isEmpty());
                return String.format("%s IN (:%s)", fieldPath, alias);
            case NOT:
                Preconditions.checkState(!filterPredicate.getValues().isEmpty());
                return String.format("%s NOT IN (:%s)", fieldPath, alias);
            case PREFIX:
                return String.format("%s LIKE CONCAT(:%s, '%%')", fieldPath, alias);
            case PREFIX_CASE_INSENSITIVE:
                return String.format("lower(%s) LIKE CONCAT(lower(:%s), '%%')", fieldPath, alias);
            case POSTFIX:
                return String.format("%s LIKE CONCAT('%%', :%s)", fieldPath, alias);
            case POSTFIX_CASE_INSENSITIVE:
                return String.format("lower(%s) LIKE CONCAT('%%', lower(:%s))", fieldPath, alias);
            case INFIX:
                return String.format("%s LIKE CONCAT('%%', :%s, '%%')", fieldPath, alias);
            case INFIX_CASE_INSENSITIVE:
                return String.format("lower(%s) LIKE CONCAT('%%', lower(:%s), '%%')", fieldPath, alias);
            case ISNULL:
                return String.format("%s IS NULL", fieldPath);
            case NOTNULL:
                return String.format("%s IS NOT NULL", fieldPath);
            case LT:
                return String.format("%s < :%s", fieldPath, alias);
            case LE:
                return String.format("%s <= :%s", fieldPath, alias);
            case GT:
                return String.format("%s > :%s", fieldPath, alias);
            case GE:
                return String.format("%s >= :%s", fieldPath, alias);
            case TRUE:
                return "(1 = 1)";
            case FALSE:
                return "(1 = 0)";

            default:
                throw new InvalidPredicateException("Operator not implemented: " + filterPredicate.getOperator());
        }
    }

    @Override
    public String applyAll(Set<FilterPredicate> filterPredicates) {
        StringBuilder filterString = new StringBuilder();

        for (FilterPredicate filterPredicate : filterPredicates) {
            if (filterString.length() == 0) {
                filterString.append("WHERE ");
            } else {
                filterString.append(" AND ");
            }

            filterString.append(apply(filterPredicate));
        }

        return filterString.toString();
    }

    public String apply(FilterExpression filterExpression) {
        HQLQueryVisitor visitor = new HQLQueryVisitor();
        return "WHERE " + filterExpression.accept(visitor);

    }

    /**
     * Filter expression visitor which builds an HQL query.
     */
    public class HQLQueryVisitor implements Visitor<String> {

        private String query;

        @Override
        public String visitPredicate(FilterPredicate filterPredicate) {
            query = apply(filterPredicate);
            return query;
        }

        @Override
        public String visitAndExpression(AndFilterExpression expression) {
            String left = expression.getLeft().accept(this);
            String right = expression.getRight().accept(this);
            query = "(" + left + " AND " + right + ")";
            return query;
        }

        @Override
        public String visitOrExpression(OrFilterExpression expression) {
            String left = expression.getLeft().accept(this);
            String right = expression.getRight().accept(this);
            query = "(" + left + " OR " + right + ")";
            return query;
        }

        @Override
        public String visitNotExpression(NotFilterExpression expression) {
            String negated = expression.getNegated().accept(this);
            query = "NOT (" + negated + ")";
            return query;
        }
    }
}
