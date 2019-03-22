/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.InMemoryFilterExecutor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Data Store Transaction that wraps another transaction and provides in-memory filtering, soring, and pagination
 * when the underlying transaction cannot perform the equivalent function.
 */
public class InMemoryStoreTransaction implements DataStoreTransaction {
    private DataStoreTransaction tx;

    boolean alwaysFilterInMemory;
    boolean alwaysSortInMemory;
    boolean alwaysPaginateInMemory;

    public InMemoryStoreTransaction(DataStoreTransaction tx, boolean filter, boolean sort, boolean paginate) {
        this.tx = tx;
        alwaysFilterInMemory = filter;
        alwaysSortInMemory = sort;

        /* We must paginate in memory if we are sorting in memory */
        alwaysPaginateInMemory = (paginate || sort);
    }

    public InMemoryStoreTransaction(DataStoreTransaction tx) {
        this(tx, false, false, false);
    }

    @Override
    public void save(Object entity, RequestScope scope) {
        tx.save(entity, scope);
    }

    @Override
    public void delete(Object entity, RequestScope scope) {
        tx.delete(entity, scope);
    }

    @Override
    public User accessUser(Object opaqueUser) {
        return tx.accessUser(opaqueUser);
    }

    @Override
    public void preCommit() {
        tx.preCommit();
    }

    @Override
    public <T> T createNewObject(Class<T> entityClass) {
        return tx.createNewObject(entityClass);
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity,
                              String relationName,
                              Optional<FilterExpression> filterExpression,
                              Optional<Sorting> sorting,
                              Optional<Pagination> pagination,
                              RequestScope scope) {

        Object result = tx.getRelation(
                relationTx,
                entity,
                relationName,
                (alwaysFilterInMemory ? Optional.empty() : filterExpression),
                (alwaysSortInMemory ? Optional.empty() : sorting),
                (alwaysPaginateInMemory ? Optional.empty() : pagination),
                scope
        );

        if (result instanceof Iterable) {
            Iterable<Object> records = (Iterable<Object>) result;

            Class<?> relationClass = scope.getDictionary().getParameterizedType(entity, relationName);
            return filterSortAndPaginateLoadedData(records, relationClass, filterExpression,
                    sorting, pagination, scope);
        }

        return result;
    }

    @Override
    public void updateToManyRelation(DataStoreTransaction relationTx,
                                     Object entity,
                                     String relationName,
                                     Set<Object> newRelationships,
                                     Set<Object> deletedRelationships,
                                     RequestScope scope) {
        tx.updateToManyRelation(relationTx, entity, relationName, newRelationships, deletedRelationships, scope);
    }

    @Override
    public void updateToOneRelation(DataStoreTransaction relationTx,
                                    Object entity,
                                    String relationName,
                                    Object relationshipValue,
                                    RequestScope scope) {
        tx.updateToOneRelation(relationTx, entity, relationName, relationshipValue, scope);
    }

    @Override
    public Object getAttribute(Object entity, String attributeName, RequestScope scope) {
        return tx.getAttribute(entity, attributeName, scope);
    }

    @Override
    public void setAttribute(Object entity, String attributeName, Object attributeValue, RequestScope scope) {
        tx.setAttribute(entity, attributeName, attributeValue, scope);

    }

    @Override
    public void flush(RequestScope scope) {
        tx.flush(scope);
    }

    @Override
    public void commit(RequestScope scope) {
        tx.commit(scope);
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        tx.createObject(entity, scope);
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope scope) {
        Iterable<Object> loadedRecords = tx.loadObjects(
                entityClass,
                (alwaysFilterInMemory ? Optional.empty() : filterExpression),
                (alwaysSortInMemory ? Optional.empty() : sorting),
                (alwaysPaginateInMemory ? Optional.empty() : pagination),
                scope
        );

        return filterSortAndPaginateLoadedData(loadedRecords, entityClass,
                filterExpression, sorting, pagination, scope);
    }

    @Override
    public void close() throws IOException {
        tx.close();
    }

    protected Iterable<Object> filterLoadedData(Iterable<Object> loadedRecords,
                                                Optional<FilterExpression> filterExpression,
                                                RequestScope scope) {
        if (! filterExpression.isPresent()) {
            return loadedRecords;
        }

        Predicate predicate = filterExpression.get().accept(new InMemoryFilterExecutor(scope));

        Stream objectStream = StreamSupport.stream(loadedRecords.spliterator(), false)
                            .filter(predicate::test);

        return objectStream::iterator;
    }

    protected Iterable<Object> filterSortAndPaginateLoadedData(Iterable<Object> loadedRecords,
                                                               Class<?> entityClass,
                                                               Optional<FilterExpression> filterExpression,
                                                               Optional<Sorting> sorting,
                                                               Optional<Pagination> pagination,
                                                               RequestScope scope) {
        if (alwaysFilterInMemory) {
            loadedRecords = filterLoadedData(loadedRecords, filterExpression, scope);
        }

        if (alwaysSortInMemory || alwaysPaginateInMemory) {
            loadedRecords = sortAndPaginateLoadedData(
                    loadedRecords,
                    entityClass,
                    (alwaysSortInMemory ? sorting : Optional.empty()),
                    pagination,
                    scope);
        }

        return loadedRecords;
    }

    protected Iterable<Object> sortAndPaginateLoadedData(Iterable<Object> loadedRecords,
                                                         Class<?> entityClass,
                                                         Optional<Sorting> sorting,
                                                         Optional<Pagination> pagination,
                                                         RequestScope scope) {

        //Try to skip the data copy if possible
        if (! sorting.isPresent() && ! pagination.isPresent()) {
            return loadedRecords;
        }

        EntityDictionary dictionary = scope.getDictionary();

        Map<Path, Sorting.SortOrder> sortRules = sorting
                .map((s) -> s.getValidSortingRules(entityClass, dictionary))
                .orElse(new HashMap<>());

        // No sorting required for this type & no pagination.
        if (sortRules.isEmpty() && ! pagination.isPresent()) {
            return loadedRecords;
        }
        //We need an in memory copy to sort or paginate.
        List<Object> results = StreamSupport.stream(loadedRecords.spliterator(), false).collect(Collectors.toList());

        if (! sortRules.isEmpty()) {
            results = sortInMemory(results, sortRules, scope);
        }

        if (pagination.isPresent()) {
            results = paginateInMemory(results, pagination.get());
        }

        return results;
    }

    protected List<Object> paginateInMemory(List<Object> records, Pagination pagination) {
        int offset = pagination.getOffset();
        int limit = pagination.getLimit();
        if (offset < 0 || offset >= records.size()) {
            return Collections.emptyList();
        }

        int endIdx = offset + limit;
        if (endIdx > records.size()) {
            endIdx = records.size();
        }

        if (pagination.isGenerateTotals()) {
            pagination.setPageTotals(records.size());
        }
        return records.subList(offset, endIdx);
    }

    protected List<Object> sortInMemory(List<Object> records,
                                        Map<Path, Sorting.SortOrder> sortRules,
                                        RequestScope scope) {
        //Build a comparator that handles multiple comparison rules.
        Comparator<Object> noSort = (left, right) -> 0;

        Comparator<Object> comp = sortRules.entrySet().stream()
            .map(entry -> getComparator(entry.getKey(), entry.getValue(), scope))
            .reduce(noSort, (comparator1, comparator2) -> (left, right) -> {
                int comparison = comparator1.compare(left, right);
                if (comparison == 0) {
                    return comparator2.compare(left, right);
                }
                return comparison;
            });

        records.sort(comp);
        return records;
    }

    private Comparator<Object> getComparator(Path path, Sorting.SortOrder order, RequestScope requestScope) {
        return (left, right) -> {
            Object leftCompare = left;
            Object rightCompare = right;

            // Drill down into path to find value for comparison
            for (Path.PathElement pathElement : path.getPathElements()) {
                leftCompare = PersistentResource.getValue(leftCompare, pathElement.getFieldName(), requestScope);
                rightCompare = PersistentResource.getValue(rightCompare, pathElement.getFieldName(), requestScope);
            }

            // Make sure value is comparable and perform comparison
            if (leftCompare instanceof Comparable) {
                int result = ((Comparable<Object>) leftCompare).compareTo(rightCompare);
                if (order == Sorting.SortOrder.asc) {
                    return result;
                }
                return -result;
            }

            throw new IllegalStateException("Trying to comparing non-comparable types!");
        };
    }
}
