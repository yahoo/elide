/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.search;

import com.paiondata.elide.core.filter.Operator;
import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpressionVisitor;
import com.paiondata.elide.core.filter.expression.NotFilterExpression;
import com.paiondata.elide.core.filter.expression.OrFilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.type.ClassType;
import com.google.common.base.Preconditions;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts an Elide filter expression into a Lucene Search query.
 */
public class FilterExpressionToSearchPredicate implements FilterExpressionVisitor<SearchPredicate> {

    SearchPredicateFactory predicateFactory;

    private Class<?> entityClass;

    public FilterExpressionToSearchPredicate(SearchPredicateFactory predicateFactory, Class<?> entityClass) {
        this.entityClass = entityClass;
        this.predicateFactory = predicateFactory;
    }

    @Override
    public SearchPredicate visitPredicate(FilterPredicate filterPredicate) {
        Preconditions.checkArgument(filterPredicate.getPath().getPathElements().size() == 1);
        Preconditions.checkArgument(filterPredicate.getEntityType().equals(ClassType.of(entityClass)));

        String queryString = "";

        List<String> predicateValues = filterPredicate.getValues()
                .stream()
                .map(Object::toString)
                .map(QueryParser::escape)
                .map(FilterExpressionToSearchPredicate::escapeWhiteSpace)
                .collect(Collectors.toList());

        Operator op = filterPredicate.getOperator();

        queryString = buildQueryString(op, predicateValues);

        return predicateFactory
                .simpleQueryString()
                .field(filterPredicate.getField())
                .matching(queryString)
                .toPredicate();
    }

    @Override
    public SearchPredicate visitAndExpression(AndFilterExpression expression) {
        return predicateFactory.bool()
                .must(expression.getLeft().accept(this))
                .must(expression.getRight().accept(this)).toPredicate();
    }

    @Override
    public SearchPredicate visitOrExpression(OrFilterExpression expression) {
        return predicateFactory.bool()
                .should(expression.getLeft().accept(this))
                .should(expression.getRight().accept(this))
                .toPredicate();
    }

    @Override
    public SearchPredicate visitNotExpression(NotFilterExpression expression) {
        return predicateFactory.bool()
                .mustNot(expression.getNegated().accept(this))
                .toPredicate();
    }

    private String buildQueryString(Operator op, List<String> predicateValues) {
        switch (op) {
            case INFIX_CASE_INSENSITIVE:
            case PREFIX_CASE_INSENSITIVE:
            case PREFIX:
            case INFIX: {
                return predicateValues.stream()
                        .map((str) -> str + "*")
                        .collect(Collectors.joining(" | "));
            }
            default:
                throw new IllegalArgumentException("Unsupported Predicate Operator: " + op);
        }
    }

    public static String escapeWhiteSpace(String str) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (Character.isWhitespace(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
