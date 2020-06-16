/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static com.yahoo.elide.core.DataStoreTransaction.FeatureSupport.FULL;
import static com.yahoo.elide.core.DataStoreTransaction.FeatureSupport.NONE;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.wrapped.TransactionWrapper;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.sort.SortFieldContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Performs full text search when it can.  Otherwise delegates to a wrapped transaction.
 */
public class SearchDataTransaction extends TransactionWrapper {

    private EntityDictionary dictionary;
    private FullTextEntityManager em;
    private int minNgram;
    private int maxNgram;

    public SearchDataTransaction(DataStoreTransaction tx,
                                 EntityDictionary dictionary,
                                 FullTextEntityManager em,
                                 int minNgramSize,
                                 int maxNgramSize) {
        super(tx);
        this.dictionary = dictionary;
        this.em = em;
        this.minNgram = minNgramSize;
        this.maxNgram = maxNgramSize;
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope requestScope) {
        if (!filterExpression.isPresent()) {
            return super.loadObjects(entityClass, filterExpression, sorting, pagination, requestScope);
        }

        boolean canSearch = (canSearch(entityClass, filterExpression.get()) != NONE);

        if (mustSort(sorting, entityClass)) {
            canSearch = canSearch && canSort(sorting.get(), entityClass);
        }

        if (canSearch) {
            return search(entityClass, filterExpression.get(), sorting, pagination);
        }

        return super.loadObjects(entityClass, filterExpression, sorting, pagination, requestScope);
    }

    /**
     * Indicates whether sorting has been requested for this entity.
     * @param sorting An optional elide sorting clause.
     * @param entityClass The entity to sort.
     * @return True if the entity must be sorted. False otherwise.
     */
    private boolean mustSort(Optional<Sorting> sorting, Class<?> entityClass) {
        return sorting.isPresent() && !sorting.get().getValidSortingRules(entityClass, dictionary).isEmpty();
    }

    /**
     * Returns whether or not Lucene can be used to sort the query.
     * @param sorting The elide sorting clause
     * @param entityClass The entity being sorted.
     * @return true if Lucene can sort.  False otherwise.
     */
    private boolean canSort(Sorting sorting, Class<?> entityClass) {

        for (Map.Entry<Path, Sorting.SortOrder> entry
                : sorting.getValidSortingRules(entityClass, dictionary).entrySet()) {

            Path path = entry.getKey();

            if (path.getPathElements().size() != 1) {
                return false;
            }

            Path.PathElement last = path.lastElement().get();
            String fieldName = last.getFieldName();

            if (dictionary.getAttributeOrRelationAnnotation(entityClass, SortableField.class, fieldName) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Builds a lucene Sort object from and Elide Sorting object.
     * @param sorting Elide sorting object
     * @param entityClass The entity being sorted.
     * @return A lucene Sort object
     */
    private Sort buildSort(Sorting sorting, Class<?> entityClass) {
        QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity(entityClass).get();

        SortFieldContext context = null;
        for (Map.Entry<Path, Sorting.SortOrder> entry
                : sorting.getValidSortingRules(entityClass, dictionary).entrySet()) {

            String fieldName = entry.getKey().lastElement().get().getFieldName();

            SortableField sortableField =
                    dictionary.getAttributeOrRelationAnnotation(entityClass, SortableField.class, fieldName);

            fieldName = sortableField.forField().isEmpty() ? fieldName : sortableField.forField();

            if (context == null) {
                context = builder.sort().byField(fieldName);
            } else {
                context.andByField(fieldName);
            }

            Sorting.SortOrder order = entry.getValue();

            if (order == Sorting.SortOrder.asc) {
                context = context.asc();
            } else {
                context = context.desc();
            }
        }
        if (context == null) {
            throw new IllegalStateException("Invalid Sort rules");
        }

        return context.createSort();
    }

    @Override
    public FeatureSupport supportsFiltering(Class<?> entityClass, FilterExpression expression) {

        /* Return the least support among all the predicates */
        FeatureSupport support = canSearch(entityClass, expression);

        if (support == NONE) {
            return super.supportsFiltering(entityClass, expression);
        }

        return support;
    }

    private DataStoreTransaction.FeatureSupport canSearch(Class<?> entityClass, FilterExpression expression) {

        /* Collapse the filter expression to a list of leaf predicates */
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        /* Return the least support among all the predicates */
        FeatureSupport support = predicates.stream()
                .map((predicate) -> canSearch(entityClass, predicate))
                .max(Comparator.comparing(Enum::ordinal)).orElse(NONE);

        if (support == NONE) {
            return support;
        }

        /* Throw an exception if ngram size is violated */
        predicates.stream().forEach((predicate) -> {
            predicate.getValues().stream().map(Object::toString).forEach((value) -> {
                if (value.length() < minNgram || value.length() > maxNgram) {
                    String message = String.format("Field values for %s on entity %s must be >= %d and <= %d",
                            predicate.getField(), dictionary.getJsonAliasFor(entityClass), minNgram, maxNgram);
                    throw new InvalidValueException(predicate.getValues(), message);
                }
            });
        });

        return support;
    }

    private DataStoreTransaction.FeatureSupport canSearch(Class<?> entityClass, FilterPredicate predicate) {

        boolean isIndexed = fieldIsIndexed(entityClass, predicate);

        if (!isIndexed) {
            return NONE;
        }

        /* We don't support joins to other relationships */
        if (predicate.getPath().getPathElements().size() != 1) {
            return NONE;
        }

        return operatorSupport(entityClass, predicate);
    }

    /**
     * Perform the full-text search.
     * @param entityClass The class to search
     * @param filterExpression The filter expression to apply
     * @param sorting Optional sorting
     * @param pagination Optional pagination
     * @return A list of records of type entityClass.
     */
    private List<Object> search(Class<?> entityClass, FilterExpression filterExpression, Optional<Sorting> sorting,
                                Optional<Pagination> pagination) {
            Query query;
            try {
                query = filterExpression.accept(new FilterExpressionToLuceneQuery(em, entityClass));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }

            FullTextQuery fullTextQuery = em.createFullTextQuery(query, entityClass);

            if (mustSort(sorting, entityClass)) {
                fullTextQuery = fullTextQuery.setSort(buildSort(sorting.get(), entityClass));
            }

            if (pagination.isPresent()) {
                fullTextQuery = fullTextQuery.setMaxResults(pagination.get().getLimit());
                fullTextQuery = fullTextQuery.setFirstResult(pagination.get().getOffset());
            }

            List<Object[]> results = fullTextQuery
                    .setProjection(ProjectionConstants.THIS)
                    .getResultList();

            if (pagination.isPresent() && pagination.get().isGenerateTotals()) {
                pagination.get().setPageTotals(fullTextQuery.getResultSize());
            }

            if (results.isEmpty()) {
                return Collections.emptyList();
            }

            return results.stream()
                    .map((result) -> {
                        return result[0];
                    }).collect(Collectors.toList());
    }

    private boolean fieldIsIndexed(Class<?> entityClass, FilterPredicate predicate) {
        String fieldName = predicate.getField();

        List<Field> fields = new ArrayList<>();

        Field fieldAnnotation = dictionary.getAttributeOrRelationAnnotation(entityClass, Field.class, fieldName);

        if (fieldAnnotation != null) {
            fields.add(fieldAnnotation);
        } else {
            Fields fieldsAnnotation =
                    dictionary.getAttributeOrRelationAnnotation(entityClass, Fields.class, fieldName);

            if (fieldsAnnotation != null) {
                Arrays.stream(fieldsAnnotation.value()).forEach(fields::add);
            }
        }

        boolean indexed = false;

        for (Field field : fields) {
            if (field.index() == Index.YES && (field.name().equals(fieldName) || field.name().isEmpty())) {
                indexed = true;
            }
        }

        return indexed;
    }

    private DataStoreTransaction.FeatureSupport operatorSupport(Class<?> entityClass, FilterPredicate predicate)
            throws HttpStatusException {

        Operator op = predicate.getOperator();

        /* We only support INFIX & PREFIX */
        switch (op) {
            case INFIX:
            case INFIX_CASE_INSENSITIVE:
                return FULL;
            case PREFIX:
            case PREFIX_CASE_INSENSITIVE:
                return FeatureSupport.PARTIAL;
            default:
                return NONE;
        }
    }
}
