/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterPredicatePushdownExtractor;
import com.yahoo.elide.core.filter.expression.InMemoryExecutionVerifier;
import com.yahoo.elide.core.filter.expression.InMemoryFilterExecutor;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
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

    private final DataStoreTransaction tx;
    private static final Comparator<Object> NULL_SAFE_COMPARE = (a, b) -> {
        if (a == null && b == null) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        } else if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        } else {
            throw new IllegalStateException("Trying to comparing non-comparable types!");
        }
    };

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
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity,
                              Relationship relationship,
                              RequestScope scope) {
        DataFetcher fetcher = new DataFetcher() {
            @Override
            public Object fetch(Optional<FilterExpression> filterExpression,
                                Optional<Sorting> sorting,
                                Optional<Pagination> pagination,
                                RequestScope scope) {

                return tx.getRelation(relationTx, entity, relationship.copyOf()
                        .projection(relationship.getProjection().copyOf()
                                .filterExpression(filterExpression.orElse(null))
                                .sorting(sorting.orElse(null))
                                .pagination(pagination.orElse(null))
                                .build()
                        ).build(), scope);
            }
        };


        /*
         * If we are mutating multiple entities, the data store transaction cannot perform filter & pagination directly.
         * It must be done in memory by Elide as some newly created entities have not yet been persisted.
         */
        boolean filterInMemory = scope.getNewPersistentResources().size() > 0;
        return fetchData(fetcher, Optional.of(entity), relationship.getProjection(), filterInMemory, scope);
    }

    @Override
    public Object loadObject(EntityProjection projection,
                      Serializable id,
                      RequestScope scope) {

        if (projection.getFilterExpression() == null
                || tx.supportsFiltering(scope, Optional.empty(), projection) == FeatureSupport.FULL) {
            return tx.loadObject(projection, id, scope);
        } else {
            return DataStoreTransaction.super.loadObject(projection, id, scope);
        }
    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection projection,
                                        RequestScope scope) {

        DataFetcher fetcher = new DataFetcher() {
            @Override
            public Iterable<Object> fetch(Optional<FilterExpression> filterExpression,
                                          Optional<Sorting> sorting,
                                          Optional<Pagination> pagination,
                                          RequestScope scope) {

                return tx.loadObjects(projection.copyOf()
                        .filterExpression(filterExpression.orElse(null))
                        .pagination(pagination.orElse(null))
                        .sorting(sorting.orElse(null))
                        .build(), scope);
            }
        };

        return (Iterable<Object>) fetchData(fetcher, Optional.empty(), projection, false, scope);
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
    public void preCommit() {
        tx.preCommit();
    }

    @Override
    public <T> T createNewObject(Class<T> entityClass) {
        return tx.createNewObject(entityClass);
    }

    @Override
    public void close() throws IOException {
        tx.close();
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
    public Object getAttribute(Object entity, Attribute attribute, RequestScope scope) {
        return tx.getAttribute(entity, attribute, scope);
    }

    @Override
    public void setAttribute(Object entity, Attribute attribute, RequestScope scope) {
        tx.setAttribute(entity, attribute, scope);
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
                               Optional<Object> parent,
                               EntityProjection projection,
                               boolean filterInMemory,
                               RequestScope scope) {

        Optional<FilterExpression> filterExpression = Optional.ofNullable(projection.getFilterExpression());

        Pair<Optional<FilterExpression>, Optional<FilterExpression>> expressionSplit = splitFilterExpression(
                scope, parent, projection, filterInMemory);

        Optional<FilterExpression> dataStoreFilter = expressionSplit.getLeft();
        Optional<FilterExpression> inMemoryFilter = expressionSplit.getRight();

        Pair<Optional<Sorting>, Optional<Sorting>> sortSplit = splitSorting(scope, parent,
                projection, inMemoryFilter.isPresent());

        Optional<Sorting> dataStoreSort = sortSplit.getLeft();
        Optional<Sorting> inMemorySort = sortSplit.getRight();

        Pair<Optional<Pagination>, Optional<Pagination>> paginationSplit = splitPagination(scope, parent,
                 projection, inMemoryFilter.isPresent(), inMemorySort.isPresent());

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
                    inMemorySort,
                    inMemoryPagination,
                    scope);
    }


    private Iterable<Object> sortAndPaginateLoadedData(Iterable<Object> loadedRecords,
                                                         Optional<Sorting> sorting,
                                                         Optional<Pagination> pagination,
                                                         RequestScope scope) {

        //Try to skip the data copy if possible
        if (! sorting.isPresent() && ! pagination.isPresent()) {
            return loadedRecords;
        }

        Map<Path, Sorting.SortOrder> sortRules = sorting
                .map(Sorting::getSortingPaths)
                .orElseGet(HashMap::new);

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

        if (pagination.returnPageTotals()) {
            pagination.setPageTotals((long) records.size());
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
                leftCompare = (leftCompare == null ? null
                        : PersistentResource.getValue(leftCompare, pathElement.getFieldName(), requestScope));
                rightCompare = (rightCompare == null ? null
                        : PersistentResource.getValue(rightCompare, pathElement.getFieldName(), requestScope));
            }

            if (order == Sorting.SortOrder.asc) {
                return NULL_SAFE_COMPARE.compare(leftCompare, rightCompare);
            }
            return NULL_SAFE_COMPARE.compare(rightCompare, leftCompare);
        };
    }

    /**
     * Splits a filter expression into two components:
     *  - a component that should be pushed down to the data store
     *  - a component that should be executed in memory
     * @param scope The request context
     * @param parent If this is a relationship load, the parent object.  Otherwise not set.
     * @param projection The projection being loaded.
     * @param filterInMemory Whether or not the transaction requires in memory filtering.
     * @return A pair of filter expressions (data store expression, in memory expression)
     */
    private Pair<Optional<FilterExpression>, Optional<FilterExpression>> splitFilterExpression(
            RequestScope scope,
            Optional<Object> parent,
            EntityProjection projection,
            boolean filterInMemory
    ) {

        Optional<FilterExpression> filterExpression = Optional.ofNullable(projection.getFilterExpression());
        Optional<FilterExpression> inStoreFilterExpression = filterExpression;
        Optional<FilterExpression> inMemoryFilterExpression = Optional.empty();

        boolean transactionNeedsInMemoryFiltering = filterInMemory;

        if (filterExpression.isPresent()) {
            FeatureSupport filterSupport = tx.supportsFiltering(scope, parent, projection);

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
     * @param scope The request context
     * @param parent If this is a relationship load, the parent object.  Otherwise not set.
     * @param projection The projection being loaded.
     * @param filteredInMemory Whether or not filtering was performed in memory
     * @return A pair of sorting objects (data store sort, in memory sort)
     */
    private Pair<Optional<Sorting>, Optional<Sorting>> splitSorting(
            RequestScope scope,
            Optional<Object> parent,
            EntityProjection projection,
            boolean filteredInMemory
    ) {
        Optional<Sorting> sorting = Optional.ofNullable(projection.getSorting());

        if (sorting.isPresent() && (! tx.supportsSorting(scope, parent, projection) || filteredInMemory)) {
            return Pair.of(Optional.empty(), sorting);
        }
        return Pair.of(sorting, Optional.empty());
    }

    /**
     * Splits a pagination object into two components:
     *  - a component that should be pushed down to the data store
     *  - a component that should be executed in memory
     * @param scope The request context
     * @param parent If this is a relationship load, the parent object.  Otherwise not set.
     * @param projection The projection being loaded.
     * @param filteredInMemory Whether or not filtering was performed in memory
     * @param sortedInMemory Whether or not sorting was performed in memory
     * @return A pair of pagination objects (data store pagination, in memory pagination)
     */
    private Pair<Optional<Pagination>, Optional<Pagination>> splitPagination(
            RequestScope scope,
            Optional<Object> parent,
            EntityProjection projection,
            boolean filteredInMemory,
            boolean sortedInMemory
    ) {

        Optional<Pagination> pagination = Optional.ofNullable(projection.getPagination());

        if (!tx.supportsPagination(scope, parent, projection)
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

   @Override
   public void cancel(RequestScope scope) {
       tx.cancel(scope);
   }
}
