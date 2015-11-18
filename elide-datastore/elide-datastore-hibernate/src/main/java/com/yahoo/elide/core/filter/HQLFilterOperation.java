/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;

import java.util.Set;

/**
 * FilterOperation that creates Hibernate query language fragments
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
}
