/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa;

import static com.paiondata.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;

import com.paiondata.elide.datastores.jpa.JpaDataStore.JpaTransactionSupplier;
import com.paiondata.elide.datastores.jpa.transaction.JpaTransaction;
import org.hibernate.Session;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.function.Consumer;

/**
 * JpaTransactionSupplier that creates PlatformJpaTransactions.
 *
 * @see PlatformJpaTransaction
 */
public class PlatformJpaTransactionSupplier implements JpaTransactionSupplier {

    private final boolean delegateToInMemoryStore;

    private final PlatformTransactionManager transactionManager;

    private final TransactionDefinition transactionDefinition;

    private final EntityManagerFactory entityManagerFactory;

    private final Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();

    public PlatformJpaTransactionSupplier(TransactionDefinition transactionDefinition,
            PlatformTransactionManager transactionManager,
            EntityManagerFactory entityManagerFactory, boolean delegateToInMemoryStore) {
        this.transactionDefinition = transactionDefinition;
        this.delegateToInMemoryStore = delegateToInMemoryStore;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;

    }

    @Override
    public JpaTransaction get(EntityManager entityManager) {
        return new PlatformJpaTransaction(this.transactionManager,
                this.transactionDefinition, this.entityManagerFactory, entityManager, this.txCancel, DEFAULT_LOGGER,
                this.delegateToInMemoryStore, true);
    }
}
