/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
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
import com.yahoo.elide.datastores.jpa.porting.EntityManagerWrapper;
import com.yahoo.elide.datastores.jpa.porting.QueryWrapper;
import com.yahoo.elide.datastores.jpa.transaction.checker.PersistentCollectionChecker;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private final Set<Object> singleElementLoads;
    private final boolean delegateToInMemoryStore;

    protected AbstractJpaTransaction(EntityManager em, Consumer<EntityManager> jpaTransactionCancel,
                                     boolean delegateToInMemoryStore) {
        this.em = em;
        this.emWrapper = new EntityManagerWrapper(em);
        this.jpaTransactionCancel = jpaTransactionCancel;

        //We need to verify objects by reference equality (a == b) rather than equals equality in case the
        //same object is loaded twice from two different collections.
        this.singleElementLoads = Collections.newSetFromMap(new IdentityHashMap<>());
        this.delegateToInMemoryStore = delegateToInMemoryStore;
    }

    @Override
    public <T> void delete(T object, RequestScope scope) {
        deferredTasks.add(() -> em.remove(object));
    }

    @Override
    public <T> void save(T object, RequestScope scope) {
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
    public <T> void createObject(T entity, RequestScope scope) {

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
    public <T> T loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {

        Type<?> entityClass = projection.getType();
        FilterExpression filterExpression = projection.getFilterExpression();

        try {
            EntityDictionary dictionary = scope.getDictionary();
            Type<?> idType = dictionary.getIdType(entityClass);
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

            T result = (T) query.getQuery().getSingleResult();

            singleElementLoads.add(result);

            return result;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public <T> Iterable<T> loadObjects(
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

        if (results.size() == 1) {
            results.forEach(singleElementLoads::add);
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
            if (IS_PERSISTENT_COLLECTION.test(filteredVal)) {

                /*
                 * If there is no filtering or sorting required in the data store, and the pagination is default,
                 * return the proxy and let the ORM manage the SQL generation.
                 */
                if (filterExpression == null && sorting == null
                        && (pagination == null || (pagination.isDefaultInstance()))) {
                    if (filteredVal.size() == 1) {
                        filteredVal.forEach(singleElementLoads::add);
                    }
                    return (R) val;
                }

                RelationshipImpl relationship = new RelationshipImpl(
                        dictionary.lookupEntityClass(EntityDictionary.getType(entity)),
                        entity,
                        relation
                );

                if (pagination != null && pagination.returnPageTotals()) {
                    pagination.setPageTotals(getTotalRecords(relationship, scope.getDictionary()));
                }

                QueryWrapper query = (QueryWrapper)
                        new SubCollectionFetchQueryBuilder(relationship, dictionary, emWrapper)
                                .build();

                if (query != null) {
                    List results = query.getQuery().getResultList();
                    if (results.size() == 1) {
                        results.forEach(singleElementLoads::add);
                    }
                    return (R) results;
                }
            }
        } else {
            singleElementLoads.add(val);
        }
        return (R) val;
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
    private <T> Long getTotalRecords(AbstractHQLQueryBuilder.Relationship relationship,
                                     EntityDictionary dictionary) {

        QueryWrapper query = (QueryWrapper)
                new SubCollectionPageTotalsQueryBuilder(relationship, dictionary, emWrapper)
                        .build();

        return (Long) query.getQuery().getSingleResult();
    }

    @Override
    public void cancel(RequestScope scope) {
        jpaTransactionCancel.accept(em);
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

    private <T> boolean doInDatabase(Optional<T> parent) {
        //In-Memory delegation is disabled.
        return !delegateToInMemoryStore
                //This is a root level load (so always let the DB do as much as possible.
                || !parent.isPresent()
                //We are fetching .../book/1/authors so N = 1 in N+1.  No harm in the DB running a query.
                || (parent.isPresent() && singleElementLoads.contains(parent.get()));
    }
}
