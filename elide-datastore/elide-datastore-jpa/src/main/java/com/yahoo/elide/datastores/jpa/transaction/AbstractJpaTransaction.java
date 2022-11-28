/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.datastores.jpa.porting.EntityManagerWrapper;
import com.yahoo.elide.datastores.jpa.transaction.checker.PersistentCollectionChecker;
import com.yahoo.elide.datastores.jpql.JPQLTransaction;
import com.yahoo.elide.datastores.jpql.porting.QueryLogger;
import org.apache.commons.collections4.CollectionUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.NoResultException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.validation.ConstraintViolationException;

/**
 * Base JPA transaction implementation class.
 */
@Slf4j
public abstract class AbstractJpaTransaction extends JPQLTransaction implements JpaTransaction {

    //Data store transaction properties must be prefixed with their package name.
    public static final String ENTITY_MANAGER_PROPERTY = AbstractJpaTransaction.class.getPackage().getName()
            + ".entityManager";

    private static final Predicate<Collection<?>> IS_PERSISTENT_COLLECTION =
            new PersistentCollectionChecker();

    protected final EntityManager em;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final Consumer<EntityManager> jpaTransactionCancel;

    /**
     * Creates a new JPA transaction.
     *
     * @param em The entity manager / session.
     * @param jpaTransactionCancel A function which can cancel a session.
     * @param logger Logs queries.
     * @param isScrollEnabled Whether or not scrolling is enabled
     * @param delegateToInMemoryStore When fetching a subcollection from another multi-element collection,
     *                                whether or not to do sorting, filtering and pagination in memory - or
     *                                do N+1 queries.
     */
    protected AbstractJpaTransaction(EntityManager em, Consumer<EntityManager> jpaTransactionCancel, QueryLogger logger,
            boolean delegateToInMemoryStore, boolean isScrollEnabled) {
        super(new EntityManagerWrapper(em, logger), delegateToInMemoryStore, isScrollEnabled);
        this.em = em;
        this.jpaTransactionCancel = jpaTransactionCancel;
    }

    protected AbstractJpaTransaction(EntityManager em, Consumer<EntityManager> jpaTransactionCancel, QueryLogger logger,
            boolean delegateToInMemoryStore) {
        this(em, jpaTransactionCancel, logger, delegateToInMemoryStore, true);
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
            return super.loadObject(projection, id, scope);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void cancel(RequestScope scope) {
        jpaTransactionCancel.accept(em);
    }

    @Override
    public <T> T getProperty(String propertyName) {
        if (ENTITY_MANAGER_PROPERTY.equals(propertyName)) {
            return (T) em;
        }

        return super.getProperty(propertyName);
    }

    @Override
    protected Predicate<Collection<?>> isPersistentCollection() {
        return IS_PERSISTENT_COLLECTION;
    }
}
