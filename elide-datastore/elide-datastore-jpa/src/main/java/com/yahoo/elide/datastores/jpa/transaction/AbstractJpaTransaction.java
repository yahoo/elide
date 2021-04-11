/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.hibernate.JPQLTransaction;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.datastores.jpa.porting.EntityManagerWrapper;
import com.yahoo.elide.datastores.jpa.porting.QueryLogger;
import com.yahoo.elide.datastores.jpa.transaction.checker.PersistentCollectionChecker;

import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
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
public abstract class AbstractJpaTransaction extends JPQLTransaction implements JpaTransaction {
    private static final Predicate<Collection<?>> IS_PERSISTENT_COLLECTION =
            new PersistentCollectionChecker();

    protected final EntityManager em;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final Consumer<EntityManager> jpaTransactionCancel;

    private final Set<Object> singleElementLoads;
    private final boolean delegateToInMemoryStore;

    /**
     * Creates a new JPA transaction.
     * @param em The entity manager / session.
     * @param jpaTransactionCancel A function which can cancel a session.
     * @param logger Logs queries.
     * @param delegateToInMemoryStore When fetching a subcollection from another multi-element collection,
     *                                whether or not to do sorting, filtering and pagination in memory - or
     *                                do N+1 queries.
     */
    protected AbstractJpaTransaction(EntityManager em, Consumer<EntityManager> jpaTransactionCancel,
                                     QueryLogger logger,
                                     boolean delegateToInMemoryStore) {
        super(new EntityManagerWrapper(em, logger), false);
        this.em = em;
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
        if (CollectionUtils.isNotEmpty(deferredTasks)) {
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

        try {
            T result = super.loadObject(projection, id, scope);
            if (result != null) {
                singleElementLoads.add(result);
            }
            return result;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public <T> Iterable<T> loadObjects(
            EntityProjection projection,
            RequestScope scope) {
        Iterable<T> results = super.loadObjects(projection, scope);

        if (results instanceof Collection && ((Collection) results).size() == 1) {
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

        R val = super.getRelation(relationTx, entity, relation, scope);

        if (val instanceof Collection) {
            if (((Collection) val).size() == 1) {
                ((Collection) val).forEach(singleElementLoads::add);
            }
            return val;
        }

        singleElementLoads.add(val);
        return val;
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
                || parent.filter(singleElementLoads::contains).isPresent();
    }

    @Override
    protected Predicate<Collection<?>> isPersistentCollection() {
        return IS_PERSISTENT_COLLECTION;
    }
}
