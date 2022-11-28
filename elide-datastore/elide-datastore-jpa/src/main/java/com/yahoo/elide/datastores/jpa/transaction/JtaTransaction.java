/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.datastores.jpql.porting.QueryLogger;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

/**
 * JTA transaction implementation.
 */
@Slf4j
public class JtaTransaction extends AbstractJpaTransaction {
    private final UserTransaction transaction;

    /**
     * Creates a new JPA transaction.
     * @param entityManager The entity manager / session.
     * @param txCancel A function which can cancel a session.
     * @param logger Logs queries.
     * @param delegateToInMemoryStore When fetching a subcollection from another multi-element collection,
     *                                whether or not to do sorting, filtering and pagination in memory - or
     *                                do N+1 queries.
     */
    public JtaTransaction(EntityManager entityManager, UserTransaction transaction, Consumer<EntityManager> txCancel,
                          QueryLogger logger, boolean delegateToInMemoryStore) {
        super(entityManager, txCancel, logger, delegateToInMemoryStore);
        this.transaction = transaction;
    }

    private static UserTransaction lookupUserTransaction() {
        try {
            return (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        } catch (NamingException e) {
            log.error("Fail lookup UserTransaction from InitialContext", e);
            throw new TransactionException(e);
        }
    }

    @Override
    public void begin() {
        try {
            transaction.begin();
        } catch (Exception e) {
            log.error("Fail UserTransaction#begin()", e);
            throw new TransactionException(e);
        }
    }

    @Override
    public void commit(RequestScope scope) {
        super.commit(scope);
        try {
            transaction.commit();
        } catch (Exception e) {
            log.error("Fail UserTransaction#commit()", e);
            throw new TransactionException(e);
        }
    }

    @Override
    public void rollback() {
        super.rollback();
        try {
            transaction.rollback();
        } catch (Exception e) {
            log.error("Fail UserTransaction#rollback()", e);
        }
    }

    @Override
    public boolean isOpen() {
        try {
            return (transaction.getStatus() == Status.STATUS_ACTIVE);
        } catch (Exception e) {
            return false;
        }
    }
}
