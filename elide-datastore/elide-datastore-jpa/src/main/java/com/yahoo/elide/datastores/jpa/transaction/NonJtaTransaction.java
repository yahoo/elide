/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.jpql.porting.QueryLogger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Non-JTA transaction implementation.
 */
@Slf4j
public class NonJtaTransaction extends AbstractJpaTransaction {
    private final EntityTransaction transaction;

    /**
     * Creates a new Non-JTA, JPA transaction.
     * @param entityManager The entity manager / session.
     * @param jpaTransactionCancel A function which can cancel a session.
     */
    public NonJtaTransaction(EntityManager entityManager, Consumer<EntityManager> jpaTransactionCancel) {
        this(entityManager, jpaTransactionCancel, DEFAULT_LOGGER, false, true);
    }

    /**
     * Creates a new Non-JTA, JPA transaction.
     * @param entityManager The entity manager / session.
     * @param jpaTransactionCancel A function which can cancel a session.
     * @param logger Logs queries.
     * @param delegateToInMemoryStore When fetching a subcollection from another multi-element collection,
     *                                whether or not to do sorting, filtering and pagination in memory - or
     *                                do N+1 queries.
     * @param isScrollEnabled Enables/disables scrollable iterators.
     */
    public NonJtaTransaction(EntityManager entityManager, Consumer<EntityManager> jpaTransactionCancel,
                             QueryLogger logger,
                             boolean delegateToInMemoryStore,
                             boolean isScrollEnabled) {
        super(entityManager, jpaTransactionCancel, logger, delegateToInMemoryStore, isScrollEnabled);
        this.transaction = entityManager.getTransaction();
        entityManager.clear();
    }

    @Override
    public void begin() {
        if (!transaction.isActive()) {
            transaction.begin();
        }
    }

    @Override
    public void commit(RequestScope scope) {
        if (transaction.isActive()) {
            super.commit(scope);
            transaction.commit();
        }
    }

    @Override
    public void rollback() {
        if (transaction.isActive()) {
            try {
                super.rollback();
            } finally {
                transaction.rollback();
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        em.close();
    }

    @Override
    public boolean isOpen() {
        return transaction.isActive();
    }
}
