/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import com.google.common.base.Preconditions;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
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

        Analyzer analyzer = new WhitespaceAnalyzer();
        QueryParser queryParser = new QueryParser(filterPredicate.getField(), analyzer);

        String queryString = "";

        List<String> predicateValues = filterPredicate.getValues()
                .stream()
                .map(Object::toString)
                .map(QueryParser::escape)
                .collect(Collectors.toList());

        boolean lowerCaseTerms = false;
        switch (filterPredicate.getOperator()) {
            case IN_INSENSITIVE:
                lowerCaseTerms = true;
            case IN: {
                queryString = predicateValues.stream()
                        .map((str) -> "\"" + str + "\"")
                        .collect(Collectors.joining(" | "));
                break;
            }
            case NOT_INSENSITIVE:
                lowerCaseTerms = true;
            case NOT: {
                queryString = predicateValues.stream()
                        .map((str) -> "-\"" + str + "\"")
                        .collect(Collectors.joining(" + "));
                break;
            }
            case INFIX_CASE_INSENSITIVE:
            case PREFIX_CASE_INSENSITIVE:
                lowerCaseTerms = true;
            case PREFIX:
            case INFIX: {
                queryString = predicateValues.stream()
                        .map((str) -> str + "*")
                        .collect(Collectors.joining(" | "));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported Predicate Operator: " + filterPredicate.getOperator());
        }


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
        Query query = builder.bool()
                .should(expression.getLeft().accept(this))
                .should(expression.getRight().accept(this))
                .createQuery();

        return query;
    }

    @Override
    public Query visitNotExpression(NotFilterExpression expression) {
        return builder.bool()
                .must(expression.getNegated().accept(this))
                .not()
                .createQuery();
    }
}
