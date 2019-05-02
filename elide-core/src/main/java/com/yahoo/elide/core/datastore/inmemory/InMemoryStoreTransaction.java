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
import com.yahoo.elide.core.filter.expression.FilterPredicatePushdownExtractor;
import com.yahoo.elide.core.filter.expression.InMemoryExecutionVerifier;
import com.yahoo.elide.core.filter.expression.InMemoryFilterExecutor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Data Store Transaction that wraps another transaction and provides in-memory filtering, soring, and pagination
 * when the underlying transaction cannot perform the equivalent function.
 */
public class InMemoryStoreTransaction implements DataStoreTransaction {

    private DataStoreTransaction tx;

    /**
     * Fetches data from the store.
     */
    @FunctionalInterface
    private interface DataFetcher {
        Object fetch(Optional<FilterExpression> filterExpression,
                     Optional<Sorting> sorting,
                     Optional<Pagination> pagination,
                     RequestScope scope);
    }


    public InMemoryStoreTransaction(DataStoreTransaction tx) {
        this.tx = tx;
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

        Class<?> relationClass = scope.getDictionary().getParameterizedType(entity, relationName);

        DataFetcher fetcher = new DataFetcher() {
            @Override
            public Object fetch(Optional<FilterExpression> filterExpression,
                                Optional<Sorting> sorting,
                                Optional<Pagination> pagination,
                                RequestScope scope) {

                return tx.getRelation(relationTx, entity, relationName, filterExpression, sorting, pagination, scope);
            }
        };


        /*
         * If we are mutating multiple entities, the data store transaction cannot perform filter & pagination directly.
         * It must be done in memory by Elide as some newly created entities have not yet been persisted.
         */
        boolean filterInMemory = scope.getNewPersistentResources().size() > 0;
        return fetchData(fetcher, relationClass, filterExpression, sorting, pagination, filterInMemory, scope);
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
    public Object loadObject(Class<?> entityClass,
                      Serializable id,
                      Optional<FilterExpression> filterExpression,
                      RequestScope scope) {

        if (! filterExpression.isPresent()
                || tx.supportsFiltering(entityClass, filterExpression.get()) == FeatureSupport.FULL) {
            return tx.loadObject(entityClass, id, filterExpression, scope);
        } else {
            return DataStoreTransaction.super.loadObject(entityClass, id, filterExpression, scope);
        }
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope scope) {

        DataFetcher fetcher = new DataFetcher() {
            @Override
            public Iterable<Object> fetch(Optional<FilterExpression> filterExpression,
                                          Optional<Sorting> sorting,
                                          Optional<Pagination> pagination,
                                          RequestScope scope) {
                return tx.loadObjects(entityClass, filterExpression, sorting, pagination, scope);
            }
        };

        return (Iterable<Object>) fetchData(fetcher, entityClass,
                filterExpression, sorting, pagination, false, scope);
    }

    @Override
    public void close() throws IOException {
        tx.close();
    }

    private Iterable<Object> filterLoadedData(Iterable<Object> loadedRecords,
                                                Optional<FilterExpression> filterExpression,
                                                RequestScope scope) {
        if (! filterExpression.isPresent()) {
            return loadedRecords;
        }

        Predicate predicate = filterExpression.get().accept(new InMemoryFilterExecutor(scope));

        return StreamSupport.stream(loadedRecords.spliterator(), false)
                            .filter(predicate::test)
                            .collect(Collectors.toList());
    }

    private Object fetchData(DataFetcher fetcher,
                               Class<?> entityClass,
                               Optional<FilterExpression> filterExpression,
                               Optional<Sorting> sorting,
                               Optional<Pagination> pagination,
                               boolean filterInMemory,
                               RequestScope scope) {

        Pair<Optional<FilterExpression>, Optional<FilterExpression>> expressionSplit = splitFilterExpression(
                entityClass, filterExpression, filterInMemory, scope);

        Optional<FilterExpression> dataStoreFilter = expressionSplit.getLeft();
        Optional<FilterExpression> inMemoryFilter = expressionSplit.getRight();

        Pair<Optional<Sorting>, Optional<Sorting>> sortSplit = splitSorting(entityClass,
                sorting, inMemoryFilter.isPresent());

        Optional<Sorting> dataStoreSort = sortSplit.getLeft();
        Optional<Sorting> inMemorySort = sortSplit.getRight();

        Pair<Optional<Pagination>, Optional<Pagination>> paginationSplit = splitPagination(entityClass,
                pagination, inMemoryFilter.isPresent(), inMemorySort.isPresent());


        Optional<Pagination> dataStorePagination = paginationSplit.getLeft();
        Optional<Pagination> inMemoryPagination = paginationSplit.getRight();

        Object result = fetcher.fetch(dataStoreFilter, dataStoreSort, dataStorePagination, scope);

        if (! (result instanceof Iterable)) {
            return result;
        }

        Iterable<Object> loadedRecords = (Iterable<Object>) result;

        if (inMemoryFilter.isPresent()) {
            loadedRecords = filterLoadedData(loadedRecords, filterExpression, scope);
        }


        return sortAndPaginateLoadedData(
                    loadedRecords,
                    entityClass,
                    inMemorySort,
                    inMemoryPagination,
                    scope);
    }


    private Iterable<Object> sortAndPaginateLoadedData(Iterable<Object> loadedRecords,
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

    private List<Object> paginateInMemory(List<Object> records, Pagination pagination) {
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

    private List<Object> sortInMemory(List<Object> records,
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
                if (order == Sorting.SortOrder.asc) {
                    return ((Comparable<Object>) leftCompare).compareTo(rightCompare);
                }
                return ((Comparable<Object>) rightCompare).compareTo(leftCompare);
            }

            throw new IllegalStateException("Trying to comparing non-comparable types!");
        };
    }

    /**
     * Splits a filter expression into two components:
     *  - a component that should be pushed down to the data store
     *  - a component that should be executed in memory
     * @param entityClass The class to filter
     * @param filterExpression The filter expression
     * @param filterInMemory Whether or not the transaction requires in memory filtering.
     * @param scope The request context
     * @return A pair of filter expressions (data store expression, in memory expression)
     */
    private Pair<Optional<FilterExpression>, Optional<FilterExpression>> splitFilterExpression(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            boolean filterInMemory,
            RequestScope scope
    ) {

        Optional<FilterExpression> inStoreFilterExpression = filterExpression;
        Optional<FilterExpression> inMemoryFilterExpression = Optional.empty();

        boolean transactionNeedsInMemoryFiltering = filterInMemory;

        if (filterExpression.isPresent()) {
            FeatureSupport filterSupport = tx.supportsFiltering(entityClass, filterExpression.get());

            boolean storeNeedsInMemoryFiltering = filterSupport != FeatureSupport.FULL;

            if (transactionNeedsInMemoryFiltering || filterSupport == FeatureSupport.NONE) {
                inStoreFilterExpression = Optional.empty();
            } else {
                inStoreFilterExpression = Optional.ofNullable(
                        FilterPredicatePushdownExtractor.extractPushDownPredicate(scope.getDictionary(),
                                filterExpression.get()));
            }

            boolean expressionNeedsInMemoryFiltering = InMemoryExecutionVerifier.shouldExecuteInMemory(
                    scope.getDictionary(), filterExpression.get());

            if (transactionNeedsInMemoryFiltering || storeNeedsInMemoryFiltering || expressionNeedsInMemoryFiltering) {
                inMemoryFilterExpression = filterExpression;
            }
        }

        return Pair.of(inStoreFilterExpression, inMemoryFilterExpression);
    }

    /**
     * Splits a sorting object into two components:
     *  - a component that should be pushed down to the data store
     *  - a component that should be executed in memory
     * @param entityClass The class to filter
     * @param sorting The sorting object
     * @param filteredInMemory Whether or not filtering was performed in memory
     * @return A pair of sorting objects (data store sort, in memory sort)
     */
    private Pair<Optional<Sorting>, Optional<Sorting>> splitSorting(
            Class<?> entityClass,
            Optional<Sorting> sorting,
            boolean filteredInMemory
    ) {
        if (sorting.isPresent() && (! tx.supportsSorting(entityClass, sorting.get()) || filteredInMemory)) {
            return Pair.of(Optional.empty(), sorting);
        } else {
            return Pair.of(sorting, Optional.empty());
        }
    }

    /**
     * Splits a pagination object into two components:
     *  - a component that should be pushed down to the data store
     *  - a component that should be executed in memory
     * @param entityClass The class to filter
     * @param pagination The pagination object
     * @param filteredInMemory Whether or not filtering was performed in memory
     * @param sortedInMemory Whether or not sorting was performed in memory
     * @return A pair of pagination objects (data store pagination, in memory pagination)
     */
    private Pair<Optional<Pagination>, Optional<Pagination>> splitPagination(
            Class<?> entityClass,
            Optional<Pagination> pagination,
            boolean filteredInMemory,
            boolean sortedInMemory
    ) {
        if (!tx.supportsPagination(entityClass)
                || filteredInMemory
                || sortedInMemory) {
            return Pair.of(Optional.empty(), pagination);

        /*
         * For default pagination, we let the store do its work, but we also let the store ignore pagination
         * by also performing in memory.  This allows the ORM the opportunity to manage its own SQL query generation
         * to avoid N+1.
         */
        } else if (pagination.isPresent() && pagination.get().isDefaultInstance()) {
            return Pair.of(pagination, pagination);
        } else {
            return Pair.of(pagination, Optional.empty());
        }
    }
}
