/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FalsePredicate;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Pagination.Direction;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.request.Sorting.SortOrder;
import com.yahoo.elide.core.security.obfuscation.IdObfuscator;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.TimedFunction;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.ScrollableIteratorBase;
import com.yahoo.elide.datastores.jpql.porting.Session;
import com.yahoo.elide.datastores.jpql.query.AbstractHQLQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.CursorEncoder;
import com.yahoo.elide.datastores.jpql.query.RelationshipImpl;
import com.yahoo.elide.datastores.jpql.query.RootCollectionFetchQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.RootCollectionPageTotalsQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.SubCollectionFetchQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.SubCollectionPageTotalsQueryBuilder;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Hibernate Transaction implementation.
 */
public abstract class JPQLTransaction implements DataStoreTransaction {

    private final Session sessionWrapper;
    private final boolean isScrollEnabled;
    private final Set<Object> singleElementLoads;
    private final boolean delegateToInMemoryStore;
    private final CursorEncoder cursorEncoder;


    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param delegateToInMemoryStore Whether to delegate to in memory store
     * @param isScrollEnabled Whether or not scrolling is enabled
     * @param cursorEncoder the cursor encoder
     */
    protected JPQLTransaction(Session session, boolean delegateToInMemoryStore, boolean isScrollEnabled,
            CursorEncoder cursorEncoder) {
        this.sessionWrapper = session;
        this.isScrollEnabled = isScrollEnabled;

        // We need to verify objects by reference equality (a == b) rather than equals equality in case the
        // same object is loaded twice from two different collections.
        this.singleElementLoads = Collections.newSetFromMap(new IdentityHashMap<>());
        this.delegateToInMemoryStore = delegateToInMemoryStore;
        this.cursorEncoder = cursorEncoder;
    }

    /**
     * load a single record with id and filter.
     *
     * @param projection The projection to query
     * @param id id of the query object
     * @param scope Request scope associated with specific request
     */
    @Override
    public <T> T loadObject(EntityProjection projection,
            Serializable id,
            RequestScope scope) {

        Type<?> entityClass = projection.getType();
        FilterExpression filterExpression = projection.getFilterExpression();

        EntityDictionary dictionary = scope.getDictionary();
        Type<?> entityIdType = dictionary.getEntityIdType(entityClass);
        Type<?> idType;
        String idField;

        if (entityIdType == null) {
            idType = dictionary.getIdType(entityClass);
            idField = dictionary.getIdFieldName(entityClass);
        } else {
            // handling for entity id
            idType = entityIdType;
            idField = dictionary.getEntityIdFieldName(entityClass);
        }

        // Construct a predicate that selects an individual element of the relationship's parent (Author.id = 3).
        FilterPredicate idExpression;
        Path.PathElement idPath = new Path.PathElement(entityClass, idType, idField);
        if (id != null) {
            idExpression = new InPredicate(idPath, id);
        } else {
            idExpression = new FalsePredicate(idPath);
        }

        FilterExpression joinedExpression = (filterExpression != null)
                ? new AndFilterExpression(filterExpression, idExpression)
                : idExpression;

        projection = projection
                .copyOf()
                .filterExpression(joinedExpression)
                .build();

        Query query =
                new RootCollectionFetchQueryBuilder(projection, dictionary, sessionWrapper, cursorEncoder).build();

        T loaded = new TimedFunction<T>(() -> query.uniqueResult(), "Query Hash: " + query.hashCode()).get();

        if (loaded != null) {
            singleElementLoads.add(loaded);
        }
        return loaded;
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(
            EntityProjection projection,
            RequestScope scope) {

        Pagination pagination = projection.getPagination();

        final Query query =
                new RootCollectionFetchQueryBuilder(projection, scope.getDictionary(), sessionWrapper, cursorEncoder)
                        .build();

        Iterable<T> results = new TimedFunction<Iterable<T>>(() -> {
            return isScrollEnabled ? query.scroll() : query.list();
        }, "Query Hash: " + query.hashCode()).get();

        final boolean hasResults;
        if (results instanceof Collection) {
            hasResults = !((Collection) results).isEmpty();
        } else if (results instanceof Iterator) {
            hasResults = ((Iterator) results).hasNext();
        } else {
            hasResults = results.iterator().hasNext();
        }

        if (pagination != null) {
            // Issue #1429
            if (pagination.returnPageTotals()) {
                if ((hasResults || pagination.getLimit() == 0)) {
                    pagination.setPageTotals(getTotalRecords(projection, scope.getDictionary()));
                } else {
                    pagination.setPageTotals(0L);
                }
            }
            // Cursor Pagination handling
            if (pagination.getDirection() != null && hasResults) {
                List list = new ArrayList();
                results.forEach(list::add);
                boolean hasNext = list.size() > pagination.getLimit();
                if (Direction.BACKWARD.equals(pagination.getDirection())) {
                    if (hasNext) {
                        // Remove the last element as it was requested to tell whether there was a next
                        // or previous page
                        list.remove(pagination.getLimit());
                    }
                    Collections.reverse(list);
                    pagination.setHasPreviousPage(hasNext);
                } else if (Direction.FORWARD.equals(pagination.getDirection())) {
                    if (hasNext) {
                        // Remove the last element as it was requested to tell whether there was a next
                        // or previous page
                        list.remove(pagination.getLimit());
                    }
                    pagination.setHasNextPage(hasNext);
                }
                if (!list.isEmpty()) {
                    Object first = list.get(0);
                    Object last = list.get(list.size() - 1);
                    String startCursor = getCursor(first, projection, scope);
                    String endCursor = getCursor(last, projection, scope);
                    pagination.setStartCursor(startCursor);
                    pagination.setEndCursor(endCursor);
                }
                results = list;
            }
        }

        return new DataStoreIterableBuilder<T>(addSingleElement(results)).build();
    }

    protected String getCursor(Object object, EntityProjection projection,
            RequestScope scope) {
        Map<String, String> keyset = getKeyset(object, projection, scope);
        return cursorEncoder.encode(keyset);
    }

    protected Map<String, String> getKeyset(Object object, EntityProjection projection,
            RequestScope scope) {
        IdObfuscator idObfuscator = scope.getDictionary().getIdObfuscator();
        String idFieldName = null;
        if (idObfuscator != null) {
            idFieldName = scope.getDictionary().getIdFieldName(projection.getType());
        }
        Map<String, String> keyset = new LinkedHashMap<>();
        Sorting sorting = projection.getSorting();
        for (Entry<Path, SortOrder> entry : sorting.getSortingPaths().entrySet()) {
            if (entry.getKey().getPathElements().size() == 1) {
                String fieldPath = entry.getKey().getFieldPath();
                String value;
                Object property = scope.getDictionary().getValue(object, fieldPath, scope);
                if (idObfuscator != null && fieldPath.equals(idFieldName)) {
                    value = idObfuscator.obfuscate(property);
                } else {
                    value = CoerceUtil.coerce(property, String.class);
                }
                keyset.put(fieldPath, value);
            }
        }
        return keyset;
    }

    @Override
    public <T, R> DataStoreIterable<R> getToManyRelation(
            DataStoreTransaction relationTx,
            T entity,
            Relationship relation,
            RequestScope scope) {

        FilterExpression filterExpression = relation.getProjection().getFilterExpression();
        Sorting sorting = relation.getProjection().getSorting();
        Pagination pagination = relation.getProjection().getPagination();

        EntityDictionary dictionary = scope.getDictionary();
        Iterable val = (Iterable) com.yahoo.elide.core.PersistentResource.getValue(entity, relation.getName(), scope);

        //If the query is safe for N+1 and the value is an ORM managed, persistent collection, run a JPQL query...
        if (doInDatabase(entity) && val instanceof Collection && isPersistentCollection().test((Collection<?>) val)) {

            /*
             * If there is no filtering or sorting required in the data store, and the pagination is default,
             * return the proxy and let Hibernate manage the SQL generation.
             */
            if (filterExpression == null && sorting == null
                    && (pagination == null || (pagination.isDefaultInstance()))) {
                return new DataStoreIterableBuilder<R>(addSingleElement(val)).allInMemory().build();
            }

            RelationshipImpl relationship = new RelationshipImpl(
                    dictionary.lookupEntityClass(EntityDictionary.getType(entity)),
                    entity,
                    relation);

            if (pagination != null && pagination.returnPageTotals()) {
                pagination.setPageTotals(getTotalRecords(
                        relationship,
                        scope.getDictionary()));
            }

            final Query query =
                    new SubCollectionFetchQueryBuilder(relationship, dictionary, sessionWrapper, cursorEncoder)
                            .build();

            if (query != null) {
                return new DataStoreIterableBuilder(addSingleElement(query.list())).build();
            }
        }
        return new DataStoreIterableBuilder<R>(addSingleElement(val)).allInMemory().build();
    }

    @Override
    public <T, R> R getToOneRelation(
            DataStoreTransaction relationTx,
            T entity,
            Relationship relationship,
            RequestScope scope
    ) {
        R loaded = DataStoreTransaction.super.getToOneRelation(relationTx, entity, relationship, scope);
        if (loaded != null) {
            singleElementLoads.add(loaded);
        }
        return loaded;
    }

    protected abstract Predicate<Collection<?>> isPersistentCollection();

    /**
     * Returns the total record count for a root entity and an optional filter expression.
     *
     * @param entityProjection The entity projection to count
     * @param dictionary the entity dictionary
     * @return The total row count.
     */
    private Long getTotalRecords(EntityProjection entityProjection, EntityDictionary dictionary) {


        Query query =
                new RootCollectionPageTotalsQueryBuilder(entityProjection, dictionary, sessionWrapper, cursorEncoder)
                        .build();

        return new TimedFunction<Long>(() -> query.uniqueResult(), "Query Hash: " + query.hashCode()).get();
    }

    /**
     * Returns the total record count for a entity relationship.
     *
     * @param relationship The relationship
     * @param dictionary the entity dictionary
     * @return The total row count.
     */
    private Long getTotalRecords(AbstractHQLQueryBuilder.Relationship relationship,
                                 EntityDictionary dictionary) {

        Query query =
                new SubCollectionPageTotalsQueryBuilder(relationship, dictionary, sessionWrapper, cursorEncoder)
                        .build();

        return new TimedFunction<Long>(() -> query.uniqueResult(), "Query Hash: " + query.hashCode()).get();
    }

    private <R> Iterable<R> addSingleElement(Iterable<R> results) {
        if (results instanceof ScrollableIteratorBase) {
            ((ScrollableIteratorBase<R, ?>) results).singletonElement().ifPresent(singleElementLoads::add);
        } else if (results instanceof Collection && ((Collection) results).size() == 1) {
            ((Collection) results).forEach(singleElementLoads::add);
        }

        return results;
    }

    protected <T> boolean doInDatabase(T parent) {
        // In-Memory delegation is disabled.
        return !delegateToInMemoryStore
                // We are fetching .../book/1/authors so N = 1 in N+1. No harm in the DB running a query.
                || singleElementLoads.contains(parent);
    }
}
