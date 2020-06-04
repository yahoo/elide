/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.jpa.JpaDataStore;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * Non-JTA transaction implementation.
 */
@Slf4j
public class NonJtaTransaction extends AbstractJpaTransaction {
    private final EntityTransaction transaction;
    private final JpaDataStore.JpaTransactionCancel jpaTransactionCancel; 
    public NonJtaTransaction(EntityManager entityManager, JpaDataStore.JpaTransactionCancel jpaTransactionCancel) {
        super(entityManager, jpaTransactionCancel);
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

    @Override
    public void cancel() {
	jpaTransactionCancel.cancel(em);
    }
}
