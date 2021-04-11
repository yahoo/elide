/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FalsePredicate;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.hibernate.hql.AbstractHQLQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.RelationshipImpl;
import com.yahoo.elide.core.hibernate.hql.RootCollectionFetchQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.RootCollectionPageTotalsQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.SubCollectionFetchQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.SubCollectionPageTotalsQueryBuilder;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * Hibernate Transaction implementation.
 */
public abstract class JPQLTransaction implements DataStoreTransaction {

    private final Session sessionWrapper;
    private final boolean isScrollEnabled;

    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param isScrollEnabled Whether or not scrolling is enabled
     */
    protected JPQLTransaction(Session session, boolean isScrollEnabled) {
        this.sessionWrapper = session;
        this.isScrollEnabled = isScrollEnabled;
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

        return query.uniqueResult();
    }

    @Override
    public <T> Iterable<T> loadObjects(
            EntityProjection projection,
            RequestScope scope) {

        Pagination pagination = projection.getPagination();

        final Query query =
                new RootCollectionFetchQueryBuilder(projection, scope.getDictionary(), sessionWrapper)
                        .build();

        Iterable<T> results = isScrollEnabled ? query.scroll() : query.list();
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

        return results;
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
        if (val instanceof Collection && isAbstractCollection((Collection<?>) val)) {

            /*
             * If there is no filtering or sorting required in the data store, and the pagination is default,
             * return the proxy and let Hibernate manage the SQL generation.
             */
            if (filterExpression == null && sorting == null
                    && (pagination == null || (pagination.isDefaultInstance()))) {
                return (R) val;
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
                return (R) query.list();
            }
        }
        return (R) val;
    }

    protected abstract boolean isAbstractCollection(Collection<?> collection);

    /**
     * Returns the total record count for a root entity and an optional filter expression.
     *
     * @param entityProjection The entity projection to count
     * @param dictionary the entity dictionary
     * @param <T> The type of entity
     * @return The total row count.
     */
    private Long getTotalRecords(EntityProjection entityProjection, EntityDictionary dictionary) {


        Query query =
                new RootCollectionPageTotalsQueryBuilder(entityProjection, dictionary, sessionWrapper)
                        .build();

        return query.uniqueResult();
    }

    /**
     * Returns the total record count for a entity relationship.
     *
     * @param relationship The relationship
     * @param dictionary the entity dictionary
     * @param <T> The type of entity
     * @return The total row count.
     */
    private Long getTotalRecords(AbstractHQLQueryBuilder.Relationship relationship,
            EntityDictionary dictionary) {

        Query query =
                new SubCollectionPageTotalsQueryBuilder(relationship, dictionary, sessionWrapper)
                        .build();

        return query.uniqueResult();
    }
}
