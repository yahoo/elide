/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FalsePredicate;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.TimedFunction;
import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.ScrollableIteratorBase;
import com.yahoo.elide.datastores.jpql.porting.Session;
import com.yahoo.elide.datastores.jpql.query.AbstractHQLQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.RelationshipImpl;
import com.yahoo.elide.datastores.jpql.query.RootCollectionFetchQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.RootCollectionPageTotalsQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.SubCollectionFetchQueryBuilder;
import com.yahoo.elide.datastores.jpql.query.SubCollectionPageTotalsQueryBuilder;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Optional;
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


    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param isScrollEnabled Whether or not scrolling is enabled
     */
    protected JPQLTransaction(Session session, boolean delegateToInMemoryStore, boolean isScrollEnabled) {
        this.sessionWrapper = session;
        this.isScrollEnabled = isScrollEnabled;

        // We need to verify objects by reference equality (a == b) rather than equals equality in case the
        // same object is loaded twice from two different collections.
        this.singleElementLoads = Collections.newSetFromMap(new IdentityHashMap<>());
        this.delegateToInMemoryStore = delegateToInMemoryStore;
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
        Type<?> idType = dictionary.getIdType(entityClass);
        String idField = dictionary.getIdFieldName(entityClass);

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
                new RootCollectionFetchQueryBuilder(projection, dictionary, sessionWrapper).build();

        T loaded = new TimedFunction<T>(() -> query.uniqueResult(), "Query Hash: " + query.hashCode()).get();
        return addSingleElement(loaded);
    }

    @Override
    public <T> Iterable<T> loadObjects(
            EntityProjection projection,
            RequestScope scope) {

        Pagination pagination = projection.getPagination();

        final Query query =
                new RootCollectionFetchQueryBuilder(projection, scope.getDictionary(), sessionWrapper)
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
            if (pagination.returnPageTotals() && (hasResults || pagination.getLimit() == 0)) {
                pagination.setPageTotals(getTotalRecords(projection, scope.getDictionary()));
            }
        }

        return addSingleElement(results);
    }

    @Override
    public <T, R> R getRelation(
            DataStoreTransaction relationTx,
            T entity,
            Relationship relation,
            RequestScope scope) {

        FilterExpression filterExpression = relation.getProjection().getFilterExpression();
        Sorting sorting = relation.getProjection().getSorting();
        Pagination pagination = relation.getProjection().getPagination();

        EntityDictionary dictionary = scope.getDictionary();
        Object val = com.yahoo.elide.core.PersistentResource.getValue(entity, relation.getName(), scope);
        if (val instanceof Collection && isPersistentCollection().test((Collection<?>) val)) {

            /*
             * If there is no filtering or sorting required in the data store, and the pagination is default,
             * return the proxy and let Hibernate manage the SQL generation.
             */
            if (filterExpression == null && sorting == null
                    && (pagination == null || (pagination.isDefaultInstance()))) {
                return addSingleElement((R) val);
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
                    new SubCollectionFetchQueryBuilder(relationship, dictionary, sessionWrapper)
                            .build();

            if (query != null) {
                return addSingleElement((R) query.list());
            }
        }
        return addSingleElement((R) val);
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
                new RootCollectionPageTotalsQueryBuilder(entityProjection, dictionary, sessionWrapper)
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
                new SubCollectionPageTotalsQueryBuilder(relationship, dictionary, sessionWrapper)
                        .build();

        return new TimedFunction<Long>(() -> query.uniqueResult(), "Query Hash: " + query.hashCode()).get();
    }

    private <R> R addSingleElement(R results) {
        if (results instanceof Iterable) {
            if (results instanceof ScrollableIteratorBase) {
                ((ScrollableIteratorBase<R, ?>) results).singletonElement().ifPresent(singleElementLoads::add);
            } else if (results instanceof Collection && ((Collection) results).size() == 1) {
                ((Collection) results).forEach(singleElementLoads::add);
            }
        } else if (results != null) {
            singleElementLoads.add(results);
        }
        return results;
    }

    protected <T> boolean doInDatabase(Optional<T> parent) {
        // In-Memory delegation is disabled.
        return !delegateToInMemoryStore
                // This is a root level load (so always let the DB do as much as possible.
                || !parent.isPresent()
                // We are fetching .../book/1/authors so N = 1 in N+1. No harm in the DB running a query.
                || parent.filter(singleElementLoads::contains).isPresent();
    }

    @Override
    public <T> FeatureSupport supportsFiltering(RequestScope scope, Optional<T> parent, EntityProjection projection) {
        return doInDatabase(parent) ? FeatureSupport.FULL : FeatureSupport.NONE;
    }

    @Override
    public <T> boolean supportsSorting(RequestScope scope, Optional<T> parent, EntityProjection projection) {
        return doInDatabase(parent);
    }

    @Override
    public <T> boolean supportsPagination(RequestScope scope, Optional<T> parent, EntityProjection projection) {
        return doInDatabase(parent);
    }
}
