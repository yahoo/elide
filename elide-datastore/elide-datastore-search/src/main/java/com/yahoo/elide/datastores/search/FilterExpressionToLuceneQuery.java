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

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.QueryBuilder;

import java.util.stream.Collectors;

/**
 * Converts an Elide filter expression into a Lucene Search query
 */
public class FilterExpressionToLuceneQuery implements FilterExpressionVisitor<Query> {

    private FullTextEntityManager entityManager;
    private QueryBuilder builder;
    private Class<?> entityClass;

    public FilterExpressionToLuceneQuery(FullTextEntityManager entityManager, Class<?> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        builder = entityManager.getSearchFactory().buildQueryBuilder().forEntity(entityClass).get();
    }

    @Override
    public Query visitPredicate(FilterPredicate filterPredicate) {
        Preconditions.checkArgument(filterPredicate.getPath().getPathElements().size() == 1);
        Preconditions.checkArgument(filterPredicate.getEntityType().equals(entityClass));

        String queryString = "";
        switch (filterPredicate.getOperator()) {
            case IN_INSENSITIVE:
            case IN: {
                queryString = filterPredicate.getValues()
                        .stream()
                        .map(Object::toString)
                        .map((str) -> "\"" + str + "\"")
                        .collect(Collectors.joining(" | "));
                break;
            }
            case NOT_INSENSITIVE:
            case NOT: {
                queryString = filterPredicate.getValues()
                        .stream()
                        .map(Object::toString)
                        .map((str) -> "-\"" + str + "\"")
                        .collect(Collectors.joining(" + "));
                break;
            }
            case PREFIX:
            case PREFIX_CASE_INSENSITIVE:
            case INFIX:
            case INFIX_CASE_INSENSITIVE: {
                queryString = filterPredicate.getValues()
                        .stream()
                        .map(Object::toString)
                        .map((str) -> str + "*")
                        .collect(Collectors.joining(" | "));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported Predicate Operator: " + filterPredicate.getOperator());
        }

        return builder.simpleQueryString()
                .onField(filterPredicate.getField())
                .matching(queryString)
                .createQuery();
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
