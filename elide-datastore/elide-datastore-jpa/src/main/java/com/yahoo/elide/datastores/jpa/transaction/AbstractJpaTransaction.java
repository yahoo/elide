/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.hibernate.hql.AbstractHQLQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.RelationshipImpl;
import com.yahoo.elide.core.hibernate.hql.RootCollectionFetchQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.RootCollectionPageTotalsQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.SubCollectionFetchQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.SubCollectionPageTotalsQueryBuilder;
import com.yahoo.elide.datastores.jpa.porting.EntityManagerWrapper;
import com.yahoo.elide.datastores.jpa.porting.QueryWrapper;
import com.yahoo.elide.datastores.jpa.transaction.checker.PersistentCollectionChecker;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Relationship;
import com.yahoo.elide.request.Sorting;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.validation.ConstraintViolationException;

/**
 * Base JPA transaction implementation class.
 */
@Slf4j
public abstract class AbstractJpaTransaction implements JpaTransaction {
    private static final Predicate<Collection<?>> IS_PERSISTENT_COLLECTION =
            new PersistentCollectionChecker();

    protected final EntityManager em;
    private final EntityManagerWrapper emWrapper;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final Consumer<EntityManager> jpaTransactionCancel;

    protected AbstractJpaTransaction(EntityManager em, Consumer<EntityManager> jpaTransactionCancel) {
        this.em = em;
        this.emWrapper = new EntityManagerWrapper(em);
        this.jpaTransactionCancel = jpaTransactionCancel;
    }

    @Override
    public void delete(Object object, RequestScope scope) {
        deferredTasks.add(() -> em.remove(object));
    }

    @Override
    public void save(Object object, RequestScope scope) {
        deferredTasks.add(() -> {
            if (!em.contains(object)) {
                em.merge(object);
            }
        });
    }

    @Override
    public void flush(RequestScope requestScope) {
        if (!isOpen()) {
            return;
        }
        try {
            deferredTasks.forEach(Runnable::run);
            deferredTasks.clear();
            FlushModeType flushMode = em.getFlushMode();
            if (flushMode == FlushModeType.AUTO && isOpen()) {
                em.flush();
            }
        } catch (Exception e) {
            try {
                rollback();
            } catch (RuntimeException e2) {
                e.addSuppressed(e2);
            } finally {
                log.error("Caught entity manager exception during flush", e);
            }
            if (e instanceof ConstraintViolationException) {
                throw e;
            }
            throw new TransactionException(e);
        }
    }

    @Override
    public abstract boolean isOpen();

    @Override
    public void commit(RequestScope scope) {
        flush(scope);
    }

    @Override
    public void rollback() {
        deferredTasks.clear();
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            rollback();
        }
        if (deferredTasks.size() > 0) {
            throw new IOException("Transaction not closed");
        }
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {

         deferredTasks.add(() -> {
            if (!em.contains(entity)) {
                em.persist(entity);
            }
         });
    }

    /**
     * load a single record with id and filter.
     *
     * @param projection       the projection to query
     * @param id               id of the query object
     * @param scope            Request scope associated with specific request
     */
    @Override
    public Object loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {

        Class<?> entityClass = projection.getType();
        FilterExpression filterExpression = projection.getFilterExpression();

        try {
            EntityDictionary dictionary = scope.getDictionary();
            Class<?> idType = dictionary.getIdType(entityClass);
            String idField = dictionary.getIdFieldName(entityClass);

            //Construct a predicate that selects an individual element of the relationship's parent.
            FilterPredicate idExpression;
            Path.PathElement idPath = new Path.PathElement(entityClass, idType, idField);
            if (id != null) {
                idExpression = new FilterPredicate(idPath, Operator.IN, Collections.singletonList(id));
            } else {
                idExpression = new FilterPredicate(idPath, Operator.FALSE, Collections.emptyList());
            }

            FilterExpression joinedExpression = (filterExpression != null)
                    ? new AndFilterExpression(filterExpression, idExpression)
                    : idExpression;

            projection = projection
                    .copyOf()
                    .filterExpression(joinedExpression)
                    .build();

            QueryWrapper query =
                    (QueryWrapper) new RootCollectionFetchQueryBuilder(projection, dictionary, emWrapper)
                            .build();

            return query.getQuery().getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Iterable<Object> loadObjects(
            EntityProjection projection,
            RequestScope scope) {
        Pagination pagination = projection.getPagination();

        QueryWrapper query =
                (QueryWrapper) new RootCollectionFetchQueryBuilder(projection, scope.getDictionary(), emWrapper)
                        .build();

        List results = query.getQuery().getResultList();

        if (pagination != null) {
            //Issue #1429
            if (pagination.returnPageTotals() && (!results.isEmpty() || pagination.getLimit() == 0)) {
                pagination.setPageTotals(getTotalRecords(projection, scope.getDictionary()));
            }
        }

        return results;
    }

    @Override
    public Object getRelation(
            DataStoreTransaction relationTx,
            Object entity,
            Relationship relation,
            RequestScope scope) {

        FilterExpression filterExpression = relation.getProjection().getFilterExpression();
        Sorting sorting = relation.getProjection().getSorting();
        Pagination pagination = relation.getProjection().getPagination();

        EntityDictionary dictionary = scope.getDictionary();
        Object val = com.yahoo.elide.core.PersistentResource.getValue(entity, relation.getName(), scope);
        if (val instanceof Collection) {
            Collection<?> filteredVal = (Collection<?>) val;
            if (IS_PERSISTENT_COLLECTION.test(filteredVal)) {

                /*
                 * If there is no filtering or sorting required in the data store, and the pagination is default,
                 * return the proxy and let the ORM manage the SQL generation.
                 */
                if (filterExpression == null && sorting == null
                        && (pagination == null || (pagination.isDefaultInstance()))) {
                    return val;
                }

                RelationshipImpl relationship = new RelationshipImpl(
                        dictionary.lookupEntityClass(entity.getClass()),
                        entity,
                        relation
                );

                if (pagination != null && pagination.returnPageTotals()) {
                    pagination.setPageTotals(getTotalRecords(
                            relation.getProjection(),
                            relationship,
                            scope.getDictionary()
                    ));
                }

                QueryWrapper query = (QueryWrapper)
                        new SubCollectionFetchQueryBuilder(relation.getProjection(), relationship,
                                dictionary, emWrapper)
                                .build();

                if (query != null) {
                    return query.getQuery().getResultList();
                }
            }
        }
        return val;
    }

    /**
     * Returns the total record count for a root entity and an optional filter expression.
     *
     * @param entityProjection The entity projection to count
     * @param dictionary       the entity dictionary
     * @param <T>              The type of entity
     * @return The total row count.
     */
    private <T> Long getTotalRecords(EntityProjection entityProjection,
                                     EntityDictionary dictionary) {


        QueryWrapper query = (QueryWrapper)
                new RootCollectionPageTotalsQueryBuilder(entityProjection, dictionary, emWrapper).build();

        return (Long) query.getQuery().getSingleResult();
    }

    /**
     * Returns the total record count for a entity relationship.
     *
     * @param relationship     The relationship
     * @param dictionary       the entity dictionary
     * @param <T>              The type of entity
     * @return The total row count.
     */
    private <T> Long getTotalRecords(EntityProjection entityProjection,
                                     AbstractHQLQueryBuilder.Relationship relationship,
                                     EntityDictionary dictionary) {

        QueryWrapper query = (QueryWrapper)
                new SubCollectionPageTotalsQueryBuilder(entityProjection, relationship, dictionary, emWrapper)
                        .build();

        return (Long) query.getQuery().getSingleResult();
    }

    @Override
    public void cancel(RequestScope scope) {
        jpaTransactionCancel.accept(em);
    }
}
