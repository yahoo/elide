/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import com.google.common.base.Preconditions;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.QueryBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts an Elide filter expression into a Lucene Search query.
 */
public class FilterExpressionToLuceneQuery implements FilterExpressionVisitor<Query> {

    private QueryBuilder builder;
    private Class<?> entityClass;

    public FilterExpressionToLuceneQuery(FullTextEntityManager entityManager, Class<?> entityClass) {
        this.entityClass = entityClass;
        builder = entityManager.getSearchFactory().buildQueryBuilder().forEntity(entityClass).get();
    }

    @Override
    public Query visitPredicate(FilterPredicate filterPredicate) {
        Preconditions.checkArgument(filterPredicate.getPath().getPathElements().size() == 1);
        Preconditions.checkArgument(filterPredicate.getEntityType().equals(entityClass));

        Analyzer analyzer = new KeywordAnalyzer();
        QueryParser queryParser = new QueryParser(filterPredicate.getField(), analyzer);

        String queryString = "";

        List<String> predicateValues = filterPredicate.getValues()
                .stream()
                .map(Object::toString)
                .map(QueryParser::escape)
                .map(FilterExpressionToLuceneQuery::escapeWhiteSpace)
                .collect(Collectors.toList());

        Operator op = filterPredicate.getOperator();

        boolean lowerCaseTerms = lowerCaseTerms(op);

        queryString = buildQueryString(op, predicateValues);

        queryParser.setLowercaseExpandedTerms(lowerCaseTerms);

        try {
            return queryParser.parse(queryString);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Query visitAndExpression(AndFilterExpression expression) {
        return builder.bool()
                .must(expression.getLeft().accept(this))
                .must(expression.getRight().accept(this))
                .createQuery();
    }

    @Override
    public Query visitOrExpression(OrFilterExpression expression) {
        return builder.bool()
                .should(expression.getLeft().accept(this))
                .should(expression.getRight().accept(this))
                .createQuery();
    }

    @Override
    public Query visitNotExpression(NotFilterExpression expression) {
        return builder.bool()
                .must(expression.getNegated().accept(this))
                .not()
                .createQuery();
    }

    private boolean lowerCaseTerms(Operator op) {
        switch (op) {
            case INFIX_CASE_INSENSITIVE:
            case PREFIX_CASE_INSENSITIVE:
                return true;

            default:
                return false;
        }
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
