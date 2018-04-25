/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.RequestScope;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

public class NonJtaTransaction extends AbstractJpaTransaction {
    private final EntityTransaction transaction;

    public NonJtaTransaction(EntityManager entityManager) {
        super(entityManager);
        this.transaction = entityManager.getTransaction();
    }

    @Override
    public void begin() {
        transaction.begin();
    }

    @Override
    public void commit(RequestScope scope) {
        super.commit(scope);
        transaction.commit();
    }

    @Override
    public void rollback() {
        super.rollback();
        transaction.rollback();
    }
}
