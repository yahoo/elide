/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.search;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreIterableBuilder;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.wrapped.TransactionWrapper;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.exceptions.HttpStatusException;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.filter.Operator;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.PredicateExtractionVisitor;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.request.Sorting;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.google.common.base.Preconditions;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Performs full text search when it can.  Otherwise delegates to a wrapped transaction.
 */
public class SearchDataTransaction extends TransactionWrapper {

    private enum FilterSupport {
        FULL,
        PARTIAL,
        NONE
    }

    private EntityDictionary dictionary;
    private SearchSession session;
    private int minNgram;
    private int maxNgram;
    public SearchDataTransaction(DataStoreTransaction tx,
                                 EntityDictionary dictionary,
                                 SearchSession session,
                                 int minNgramSize,
                                 int maxNgramSize) {
        super(tx);
        this.dictionary = dictionary;
        this.session = session;
        this.minNgram = minNgramSize;
        this.maxNgram = maxNgramSize;
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection projection,
                                                RequestScope requestScope) {
        if (projection.getFilterExpression() == null) {
            return super.loadObjects(projection, requestScope);
        }

        FilterSupport filterSupport = canSearch(projection.getType(), projection.getFilterExpression());
        boolean canSearch = (filterSupport != FilterSupport.NONE);

        if (mustSort(Optional.ofNullable(projection.getSorting()))) {
            canSearch = canSearch && canSort(projection.getSorting(), projection.getType());
        }

        if (canSearch) {
            Iterable<T> result = search(projection.getType(), projection.getFilterExpression(),
                    Optional.ofNullable(projection.getSorting()),
                    Optional.ofNullable(projection.getPagination()));
            if (filterSupport == FilterSupport.PARTIAL) {
                return new DataStoreIterableBuilder(result).allInMemory().build();
            } else {
                return new DataStoreIterableBuilder(result).build();
            }
        }

        return super.loadObjects(projection, requestScope);
    }

    /**
     * Indicates whether sorting has been requested for this entity.
     * @param sorting An optional elide sorting clause.
     * @return True if the entity must be sorted. False otherwise.
     */
    private boolean mustSort(Optional<Sorting> sorting) {
        return sorting.filter(s -> !s.getSortingPaths().isEmpty()).isPresent();
    }

    /**
     * Returns whether or not Lucene can be used to sort the query.
     * @param sorting The elide sorting clause
     * @param entityClass The entity being sorted.
     * @return true if Lucene can sort.  False otherwise.
     */
    private boolean canSort(Sorting sorting, Type<?> entityClass) {
        for (Map.Entry<Path, Sorting.SortOrder> entry
                : sorting.getSortingPaths().entrySet()) {

            Path path = entry.getKey();

            if (path.getPathElements().size() != 1) {
                return false;
            }

            Path.PathElement last = path.lastElement().get();
            String fieldName = last.getFieldName();

            boolean sortable = fieldIsSortable(entityClass, fieldName);

            if (! sortable) {
                return false;
            }
        }

        return true;
    }

    /**
     * Builds a lucene Sort object from and Elide Sorting object.
     * @param sorting Elide sorting object
     * @param entityType The entity being sorted.
     * @return A lucene Sort object
     */
    private SearchSort buildSort(SearchScope searchScope, Type<?> entityType, Sorting sorting) {
        SearchSortFactory sortFactory = null;
        FieldSortOptionsStep step = null;
        for (Map.Entry<Path, Sorting.SortOrder> entry : sorting.getSortingPaths().entrySet()) {

            String fieldName = entry.getKey().lastElement().get().getFieldName();

            KeywordField[] keywordFields =
                    dictionary.getAttributeOrRelationAnnotations(entityType, KeywordField.class, fieldName);

            if (keywordFields != null) {
                for (KeywordField keywordField : keywordFields) {
                    if (keywordField.sortable() == Sortable.YES && !keywordField.name().isEmpty()) {
                        fieldName = keywordField.name();
                        break;
                    }
                }
            }

            GenericField[] genericFields =
                    dictionary.getAttributeOrRelationAnnotations(entityType, GenericField.class, fieldName);

            if (genericFields != null) {
                for (GenericField genericField : genericFields) {
                    if (genericField.sortable() == Sortable.YES && !genericField.name().isEmpty()) {
                        fieldName = genericField.name();
                        break;
                    }
                }
            }

            Sorting.SortOrder order = entry.getValue();

            if (sortFactory == null) {
                sortFactory = searchScope.sort();
            } else {
                sortFactory = step.then();
            }

            if (order == Sorting.SortOrder.asc) {
                step = sortFactory.field(fieldName).asc();
            } else {
                step = sortFactory.field(fieldName).desc();
            }
        }

        return step.toSort();
    }

    private FilterSupport canSearch(Type<?> entityClass, FilterExpression expression) {

        /* Collapse the filter expression to a list of leaf predicates */
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        /* Return the least support among all the predicates */
        FilterSupport support = predicates.stream()
                .map((predicate) -> canSearch(entityClass, predicate))
                .max(Comparator.comparing(Enum::ordinal)).orElse(FilterSupport.NONE);

        if (support == FilterSupport.NONE) {
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

    private FilterSupport canSearch(Type<?> entityClass, FilterPredicate predicate) {

        boolean isIndexed = fieldIsIndexed(entityClass, predicate);

        if (!isIndexed) {
            return FilterSupport.NONE;
        }

        /* We don't support joins to other relationships */
        if (predicate.getPath().getPathElements().size() != 1) {
            return FilterSupport.NONE;
        }

        return operatorSupport(entityClass, predicate);
    }

    /**
     * Perform the full-text search.
     * @param entityType The class to search
     * @param filterExpression The filter expression to apply
     * @param sorting Optional sorting
     * @param pagination Optional pagination
     * @return A list of records of type entityClass.
     */
    private <T> List<T> search(Type<?> entityType, FilterExpression filterExpression, Optional<Sorting> sorting,
                                Optional<Pagination> pagination) {
        Class<?> entityClass = null;
        if (entityType != null) {
            Preconditions.checkState(entityType instanceof ClassType);
            entityClass = ((ClassType) entityType).getCls();
        }

        SearchScope scope = session.scope(entityClass);
        SearchPredicate predicate;
        try {
            predicate = filterExpression.accept(
                    new FilterExpressionToSearchPredicate(scope.predicate(), entityClass));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        SearchQueryOptionsStep step = session.search(entityClass).where(predicate);

        if (mustSort(sorting)) {
            SearchSort sort = buildSort(scope, entityType, sorting.get());
            step = step.sort(sort);
        }

        SearchResult result;
        if (pagination.isPresent()) {
            result = step.fetch(pagination.get().getOffset(), pagination.get().getLimit());
        } else {
            result = step.fetchAll();
        }

        if (pagination.filter(Pagination::returnPageTotals).isPresent()) {
            pagination.get().setPageTotals(result.total().hitCount());
        }

        List<T> results = result.hits();

        return results;
    }

    private boolean fieldIsSortable(Type<?> entityClass, String fieldName) {
        GenericField[] genericFields =
                dictionary.getAttributeOrRelationAnnotations(entityClass, GenericField.class, fieldName);

        if (genericFields != null) {
            for (GenericField genericField : genericFields) {
                if (genericField.sortable() == Sortable.YES) {
                    return true;
                }
            }
        }

        KeywordField[] keywordFields =
                dictionary.getAttributeOrRelationAnnotations(entityClass, KeywordField.class, fieldName);

        if (keywordFields != null) {
            for (KeywordField keywordField : keywordFields) {
                if (keywordField.sortable() == Sortable.YES) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean fieldIsIndexed(Type<?> entityClass, FilterPredicate predicate) {
        String fieldName = predicate.getField();
        FullTextField[] fullTextFields =
                dictionary.getAttributeOrRelationAnnotations(entityClass, FullTextField.class, fieldName);

        if (fullTextFields != null) {
            for (FullTextField fullTextField : fullTextFields) {
                if (fullTextField.searchable() == Searchable.YES
                        && (fullTextField.name().equals(fieldName) || fullTextField.name().isEmpty())) {
                    return true;
                }
            }
        }

        GenericField[] genericFields =
                dictionary.getAttributeOrRelationAnnotations(entityClass, GenericField.class, fieldName);

        if (genericFields != null) {
            for (GenericField genericField : genericFields) {
                if (genericField.searchable() == Searchable.YES
                        && (genericField.name().equals(fieldName) || genericField.name().isEmpty())) {
                    return true;
                }
            }
        }

        KeywordField[] keywordFields =
                dictionary.getAttributeOrRelationAnnotations(entityClass, KeywordField.class, fieldName);

        if (keywordFields != null) {
            for (KeywordField keywordField : keywordFields) {
                if (keywordField.searchable() == Searchable.YES
                        && (keywordField.name().equals(fieldName) || keywordField.name().isEmpty())) {
                    return true;
                }
            }
        }

        return false;
    }

    private FilterSupport operatorSupport(Type<?> entityClass, FilterPredicate predicate)
            throws HttpStatusException {

        Operator op = predicate.getOperator();

        /* We only support INFIX & PREFIX */
        switch (op) {
            case INFIX:
            case INFIX_CASE_INSENSITIVE:
                return FilterSupport.FULL;
            case PREFIX:
            case PREFIX_CASE_INSENSITIVE:
                return FilterSupport.PARTIAL;
            default:
                return FilterSupport.NONE;
        }
    }
}
