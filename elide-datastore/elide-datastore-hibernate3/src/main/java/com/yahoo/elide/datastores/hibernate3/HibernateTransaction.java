/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
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
import com.yahoo.elide.datastores.hibernate3.porting.QueryWrapper;
import com.yahoo.elide.datastores.hibernate3.porting.SessionWrapper;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.AbstractPersistentCollection;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;


/**
 * Hibernate Transaction implementation.
 */
@Slf4j
public class HibernateTransaction implements DataStoreTransaction {

    private final Session session;
    private final SessionWrapper sessionWrapper;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final boolean isScrollEnabled;
    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param isScrollEnabled Whether or not scrolling is enabled
     * @param scrollMode Scroll mode to use if scrolling enabled
     */
    protected HibernateTransaction(Session session, boolean isScrollEnabled, ScrollMode scrollMode) {
        this.session = session;
        this.sessionWrapper = new SessionWrapper(session);
        this.isScrollEnabled = isScrollEnabled;
    }

    @Override
    public <T> void delete(T object, RequestScope scope) {
        deferredTasks.add(() -> session.delete(object));
    }

    @Override
    public <T> void save(T object, RequestScope scope) {
        deferredTasks.add(() -> session.saveOrUpdate(object));
    }

    @Override
    public void flush(RequestScope requestScope) {
        try {
            deferredTasks.forEach(Runnable::run);
            deferredTasks.clear();
            hibernateFlush(requestScope);
        } catch (HibernateException e) {
            log.error("Caught hibernate exception during flush", e);
            throw new TransactionException(e);
        }
    }

    protected void hibernateFlush(RequestScope requestScope) {
        FlushMode flushMode = session.getFlushMode();
        if (flushMode != FlushMode.COMMIT && flushMode != FlushMode.MANUAL && flushMode != FlushMode.NEVER) {
            session.flush();
        }
    }

    @Override
    public void commit(RequestScope scope) {
        try {
            this.flush(scope);
            this.session.getTransaction().commit();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        deferredTasks.add(() -> session.persist(entity));
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

        try {
            EntityDictionary dictionary = scope.getDictionary();
            Type<?> idType = dictionary.getIdType(entityClass);
            String idField = dictionary.getIdFieldName(entityClass);

            //Construct a predicate that selects an individual element of the relationship's parent (Author.id = 3).
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

            QueryWrapper query =
                    (QueryWrapper) new RootCollectionFetchQueryBuilder(projection, dictionary, sessionWrapper).build();

            return (T) query.getQuery().uniqueResult();
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    @Override
    public <T> Iterable<T> loadObjects(
            EntityProjection projection,
            RequestScope scope) {
        Pagination pagination = projection.getPagination();

        final QueryWrapper query =
                (QueryWrapper) new RootCollectionFetchQueryBuilder(projection, scope.getDictionary(), sessionWrapper)
                        .build();

        Iterable results;
        final boolean hasResults;
        if (isScrollEnabled) {
            results = new ScrollableIterator<>(query.getQuery().scroll());
            hasResults = ((ScrollableIterator) results).hasNext();
        } else {
            results = query.getQuery().list();
            hasResults = ! ((Collection) results).isEmpty();
        }

        if (pagination != null) {
            //Issue #1429
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
        if (val instanceof Collection) {
            Collection<?> filteredVal = (Collection<?>) val;
            if (filteredVal instanceof AbstractPersistentCollection) {

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
                        relation
                );

                if (pagination != null && pagination.returnPageTotals()) {
                    pagination.setPageTotals(getTotalRecords(
                            relationship,
                            scope.getDictionary())
                    );
                }

                final QueryWrapper query =
                    (QueryWrapper) new SubCollectionFetchQueryBuilder(
                            relationship,
                            dictionary,
                            sessionWrapper)
                                .build();

                if (query != null) {
                    return (R) query.getQuery().list();
                }
            }
        }
        return (R) val;
    }

    /**
     * Returns the total record count for a root entity and an optional filter expression.
     * @param entityProjection The entity projection to count
     * @param <T> The type of entity
     * @return The total row count.
     */
    private <T> Long getTotalRecords(EntityProjection entityProjection,
                                     EntityDictionary dictionary) {

        QueryWrapper query =
                (QueryWrapper) new RootCollectionPageTotalsQueryBuilder(entityProjection,
                        dictionary, sessionWrapper).build();

        return (Long) query.getQuery().uniqueResult();
    }

    /**
     * Returns the total record count for a entity relationship.
     * @param relationship The relationship to count
     * @param <T> The type of entity
     * @return The total row count.
     */
    private <T> Long getTotalRecords(AbstractHQLQueryBuilder.Relationship relationship,
                                     EntityDictionary dictionary) {

        QueryWrapper query =
                (QueryWrapper) new SubCollectionPageTotalsQueryBuilder(
                        relationship,
                        dictionary,
                        sessionWrapper
                ).build();

        return (Long) query.getQuery().uniqueResult();
    }

    @Override
    public void close() throws IOException {
        if (session.isOpen() && session.getTransaction().isActive()) {
            session.getTransaction().rollback();
            throw new IOException("Transaction not closed");
        }
    }

    /**
     * Overrideable default query limit for the data store.
     *
     * @return default limit
     */
    public Integer getQueryLimit() {
        // no limit
        return null;
    }

    @Override
    public void cancel(RequestScope scope) {
        session.cancelQuery();
    }
}
