/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

public class JtaTransaction extends AbstractJpaTransaction {
    private static final Logger log = LoggerFactory.getLogger(JtaTransaction.class);

    private final UserTransaction transaction;

    public JtaTransaction(EntityManager entityManager) {
        this(entityManager, lookupUserTransaction());
    }

    public JtaTransaction(EntityManager entityManager, UserTransaction transaction) {
        super(entityManager);
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
}
