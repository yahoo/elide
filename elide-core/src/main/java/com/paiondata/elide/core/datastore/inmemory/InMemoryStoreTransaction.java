/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.datastore.inmemory;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreIterableBuilder;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.FilterPredicatePushdownExtractor;
import com.paiondata.elide.core.filter.expression.InMemoryExecutionVerifier;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.request.Sorting;
import com.paiondata.elide.core.type.Type;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }
        throw new IllegalStateException("Trying to comparing non-comparable types!");
    };

    /**
     * Fetches data from the store.
     */
    @FunctionalInterface
    private interface DataFetcher {
        DataStoreIterable<Object> fetch(Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting,
                                        Optional<Pagination> pagination,
                                        RequestScope scope);
    }

    public InMemoryStoreTransaction(DataStoreTransaction tx) {
        this.tx = tx;
    }

    @Override
    public DataStoreIterable<Object> getToManyRelation(DataStoreTransaction relationTx,
                                    Object entity,
                                    Relationship relationship,
                                    RequestScope scope) {
        DataFetcher fetcher = (filterExpression, sorting, pagination, requestScope) ->
                tx.getToManyRelation(relationTx, entity, relationship.copyOf()
                        .projection(relationship.getProjection().copyOf()
                                .filterExpression(filterExpression.orElse(null))
                                .sorting(sorting.orElse(null))
                                .pagination(pagination.orElse(null))
                                .build()
                        ).build(), requestScope);


        /*
         * If we are mutating multiple entities, the data store transaction cannot perform filter & pagination directly.
         * It must be done in memory by Elide as some newly created entities have not yet been persisted.
         */
        boolean filterInMemory = scope.getNewPersistentResources().size() > 0;
        return fetchData(fetcher, relationship.getProjection(), filterInMemory, scope);
    }

    @Override
    public Object loadObject(EntityProjection projection,
                      Serializable id,
                      RequestScope scope) {
        if (projection.getFilterExpression() == null) {
            return tx.loadObject(projection, id, scope);
        }

        return DataStoreTransaction.super.loadObject(projection, id, scope);
    }

    @Override
    public DataStoreIterable<Object> loadObjects(EntityProjection projection,
                                        RequestScope scope) {

        DataFetcher fetcher = (filterExpression, sorting, pagination, requestScope) ->
                tx.loadObjects(projection.copyOf()
                        .filterExpression(filterExpression.orElse(null))
                        .pagination(pagination.orElse(null))
                        .sorting(sorting.orElse(null))
                        .build(), requestScope);

        return fetchData(fetcher, projection, false, scope);
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
    public void preCommit(RequestScope scope) {
        tx.preCommit(scope);
    }

    @Override
    public <T> T createNewObject(Type<T> entityClass, RequestScope scope) {
        return tx.createNewObject(entityClass, scope);
    }

    @Override
    public <T, R> R getToOneRelation(
            DataStoreTransaction relationTx,
            T entity, Relationship relationship,
            RequestScope scope
    ) {
        return tx.getToOneRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public void close() throws IOException {
        tx.close();
    }

    @Override
    public <T, R> void updateToManyRelation(DataStoreTransaction relationTx,
                                     T entity,
                                     String relationName,
                                     Set<R> newRelationships,
                                     Set<R> deletedRelationships,
                                     RequestScope scope) {
        tx.updateToManyRelation(relationTx, entity, relationName, newRelationships, deletedRelationships, scope);
    }

    @Override
    public <T, R> void updateToOneRelation(DataStoreTransaction relationTx,
                                    T entity,
                                    String relationName,
                                    R relationshipValue,
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

    private DataStoreIterable<Object> filterLoadedData(DataStoreIterable<Object> loadedRecords,
                                                Optional<FilterExpression> filterExpression,
                                                RequestScope scope) {

        if (! filterExpression.isPresent()) {
            return loadedRecords;
        }

        return new DataStoreIterable<>() {
            @Override
            public Iterable<Object> getWrappedIterable() {
                return loadedRecords;
            }

            @Override
            public Iterator<Object> iterator() {
                return new FilteredIterator<>(filterExpression.get(), scope, loadedRecords.iterator());
            }

            @Override
            public boolean needsInMemoryFilter() {
                return true;
            }

            @Override
            public boolean needsInMemorySort() {
                return true;
            }

            @Override
            public boolean needsInMemoryPagination() {
                return true;
            }
        };
    }

    private DataStoreIterable<Object> fetchData(
            DataFetcher fetcher,
            EntityProjection projection,
            boolean filterInMemory,
            RequestScope scope
    ) {
        Optional<FilterExpression> filterExpression = Optional.ofNullable(projection.getFilterExpression());

        Pair<Optional<FilterExpression>, Optional<FilterExpression>> expressionSplit = splitFilterExpression(
                scope, projection, filterInMemory);

        Optional<FilterExpression> dataStoreFilter = expressionSplit.getLeft();
        Optional<FilterExpression> inMemoryFilter = expressionSplit.getRight();

        Optional<Sorting> dataStoreSorting = getDataStoreSorting(scope, projection, filterInMemory);

        boolean sortingInMemory = dataStoreSorting.isEmpty() && projection.getSorting() != null;

        Optional<Pagination> dataStorePagination = inMemoryFilter.isPresent() || sortingInMemory
                ? Optional.empty() : Optional.ofNullable(projection.getPagination());

        DataStoreIterable<Object> loadedRecords =
                fetcher.fetch(dataStoreFilter, dataStoreSorting, dataStorePagination, scope);

        if (loadedRecords == null) {
            return new DataStoreIterableBuilder().build();
        }

        if (inMemoryFilter.isPresent() || (loadedRecords.needsInMemoryFilter()
                && projection.getFilterExpression() != null)) {
            loadedRecords = filterLoadedData(loadedRecords, filterExpression, scope);
        }

        return sortAndPaginateLoadedData(
                    loadedRecords,
                    sortingInMemory,
                    projection.getSorting(),
                    projection.getPagination(),
                    scope);
    }

    private DataStoreIterable<Object> sortAndPaginateLoadedData(
            DataStoreIterable<Object> loadedRecords,
            boolean sortingInMemory,
            Sorting sorting,
            Pagination pagination,
            RequestScope scope
    ) {

        Map<Path, Sorting.SortOrder> sortRules = sorting == null ? new HashMap<>() : sorting.getSortingPaths();

        boolean mustSortInMemory = ! sortRules.isEmpty()
                && (sortingInMemory || loadedRecords.needsInMemorySort());

        boolean mustPaginateInMemory = pagination != null
                && (mustSortInMemory || loadedRecords.needsInMemoryPagination());

        //Try to skip the data copy if possible
        if (! mustSortInMemory && ! mustPaginateInMemory) {
            return loadedRecords;
        }

        //We need an in memory copy to sort or paginate.
        List<Object> results = StreamSupport.stream(loadedRecords.spliterator(), false).collect(Collectors.toList());

        if (! sortRules.isEmpty()) {
            results = sortInMemory(results, sortRules, scope);
        }

        if (pagination != null) {
            results = paginateInMemory(results, pagination);
        }

        return new DataStoreIterableBuilder(results).build();
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
     * Returns the sorting (if any) that should be pushed to the datastore.
     * @param scope The request context
     * @param projection The projection being loaded.
     * @param filterInMemory Whether or not the transaction requires in memory filtering.
     * @return An optional sorting.
     */
    private Optional<Sorting> getDataStoreSorting(
            RequestScope scope,
            EntityProjection projection,
            boolean filterInMemory
    ) {
        Sorting sorting = projection.getSorting();
        if (filterInMemory) {
            return Optional.empty();
        }
        Map<Path, Sorting.SortOrder> sortRules = sorting == null ? new HashMap<>() : sorting.getSortingPaths();

        boolean sortingOnComputedAttribute = false;
        for (Path path: sortRules.keySet()) {
            if (path.isComputed(scope.getDictionary())) {
                Type<?> pathType = path.getPathElements().get(0).getType();
                if (projection.getType().equals(pathType)) {
                    sortingOnComputedAttribute = true;
                    break;
                }
            }
        }
        if (sortingOnComputedAttribute) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(sorting);
        }
    }

    /**
     * Splits a filter expression into two components.  They are:
     *  - a component that should be pushed down to the data store
     *  - a component that should be executed in memory
     * @param scope The request context
     * @param projection The projection being loaded.
     * @param filterInMemory Whether or not the transaction requires in memory filtering.
     * @return A pair of filter expressions (data store expression, in memory expression)
     */
    private Pair<Optional<FilterExpression>, Optional<FilterExpression>> splitFilterExpression(
            RequestScope scope,
            EntityProjection projection,
            boolean filterInMemory
    ) {

        Optional<FilterExpression> filterExpression = Optional.ofNullable(projection.getFilterExpression());
        Optional<FilterExpression> inStoreFilterExpression = filterExpression;
        Optional<FilterExpression> inMemoryFilterExpression = Optional.empty();

        boolean transactionNeedsInMemoryFiltering = filterInMemory;

        if (filterExpression.isPresent()) {
            if (transactionNeedsInMemoryFiltering) {
                inStoreFilterExpression = Optional.empty();
            } else {
                inStoreFilterExpression = Optional.ofNullable(
                        FilterPredicatePushdownExtractor.extractPushDownPredicate(scope.getDictionary(),
                                filterExpression.get()));
            }

            boolean expressionNeedsInMemoryFiltering = InMemoryExecutionVerifier.shouldExecuteInMemory(
                    scope.getDictionary(), filterExpression.get());

            if (transactionNeedsInMemoryFiltering || expressionNeedsInMemoryFiltering) {
                inMemoryFilterExpression = filterExpression;
            }
        }

        return Pair.of(inStoreFilterExpression, inMemoryFilterExpression);
    }

   @Override
   public void cancel(RequestScope scope) {
       tx.cancel(scope);
   }

    @Override
    public <T> T getProperty(String propertyName) {
        return tx.getProperty(propertyName);
    }
}
