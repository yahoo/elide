/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.datastores.jpa.SupplierEntityManager;
import com.paiondata.elide.datastores.jpa.transaction.AbstractJpaTransaction;
import com.paiondata.elide.datastores.jpql.porting.QueryLogger;

import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.function.Consumer;

/**
 * JpaTransaction that uses the Spring PlatformTransactionManager for transaction management.
 *
 * <p>The retrieved EntityManager is closed by the PlatformTransactionManager implementation.
 * @see org.springframework.orm.jpa.JpaTransactionManager
 */
public class PlatformJpaTransaction extends AbstractJpaTransaction {

    private final PlatformTransactionManager transactionManager;

    private final TransactionDefinition definition;

    private final EntityManagerFactory entityManagerFactory;

    private TransactionStatus status;

    public PlatformJpaTransaction(PlatformTransactionManager transactionManager, TransactionDefinition definition,
            EntityManagerFactory entityManagerFactory, EntityManager em, Consumer<EntityManager> jpaTransactionCancel,
            QueryLogger logger, boolean delegateToInMemoryStore, boolean isScrollEnabled) {
        super(em, jpaTransactionCancel, logger, delegateToInMemoryStore, isScrollEnabled);
        this.transactionManager = transactionManager;
        this.definition = definition;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void begin() {
        this.status = this.transactionManager.getTransaction(this.definition);
        if (this.em instanceof SupplierEntityManager supplierEntityManager) {
            EntityManagerHolder entityManagerHolder = (EntityManagerHolder) TransactionSynchronizationManager
                    .getResource(this.entityManagerFactory);
            if (entityManagerHolder != null) {
                // This is for the JpaTransactionManager
                supplierEntityManager.setEntityManager(entityManagerHolder.getEntityManager());
            } else {
                // This is for the JtaTransactionManager
                supplierEntityManager.setEntityManager(this.entityManagerFactory.createEntityManager());
            }
        } else {
            throw new IllegalStateException("Expected entity manager to be supplied by EntityManagerProxySupplier");
        }
    }

    @Override
    public void commit(RequestScope scope) {
        if (isOpen()) {
            super.commit(scope);
            this.transactionManager.commit(this.status);
        }
    }

    @Override
    public void rollback() {
        if (isOpen()) {
            try {
                super.rollback();
            } finally {
                this.transactionManager.rollback(this.status);
            }
        }
    }

    @Override
    public boolean isOpen() {
        return this.em.isOpen() && this.status != null && !this.status.isCompleted();
    }
}
