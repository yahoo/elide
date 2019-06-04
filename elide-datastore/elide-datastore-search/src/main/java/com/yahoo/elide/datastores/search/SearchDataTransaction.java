/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.wrapped.WrappedTransaction;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextEntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Performs full text search when it can.  Otherwise delegates to a wrapped transaction.
 */
public class SearchDataTransaction extends WrappedTransaction {

    EntityDictionary dictionary;
    FullTextEntityManager em;

    public SearchDataTransaction(DataStoreTransaction tx, EntityDictionary dictionary, FullTextEntityManager em) {
        super(tx);
        this.dictionary = dictionary;
        this.em = em;
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope requestScope) {

        if (! filterExpression.isPresent()) {
            return super.loadObjects(entityClass, filterExpression, sorting, pagination, requestScope);
        }

        Collection<FilterPredicate> predicates = filterExpression.get().accept(new PredicateExtractionVisitor());

        boolean canSearch = predicates.stream()
                .allMatch((predicate) -> {
                    return predicate.getPath().getPathElements().size() == 1
                            && dictionary.getAttributeOrRelationAnnotation(predicate.getEntityType(),
                            Field.class, predicate.getField()) != null;

                });

        if (canSearch) {

            Query query;
            try {
                query = filterExpression.get().accept(new FilterExpressionToLuceneQuery(em, entityClass));
            } catch (IllegalArgumentException e) {
                throw new InvalidPredicateException(e.getMessage());
            }

            List<Object[]> results = em.createFullTextQuery(query, entityClass)
                    .setProjection(ProjectionConstants.THIS)
                    .getResultList();

            if (results.isEmpty()) {
                return new ArrayList<>();
            }

            return results.stream()
                    .map((result) -> {
                        return result[0];
                    }).collect(Collectors.toList());
        }

        return super.loadObjects(entityClass, filterExpression, sorting, pagination, requestScope);
    }
}
