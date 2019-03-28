/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.TransactionException;

import com.google.common.base.Preconditions;

import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.PersistenceException;

/**
 * Implementation for HibernateStore supporting SessionFactory.
 */
public class HibernateSessionFactoryStore extends AbstractHibernateStore {

    public HibernateSessionFactoryStore(SessionFactory aSessionFactory,
                                           boolean isScrollEnabled,
                                           ScrollMode scrollMode) {
        super(aSessionFactory, isScrollEnabled, scrollMode);
    }

    /**
     * Get current Hibernate session.
     *
     * @return session
     */
    @Override
    public Session getSession() {
        try {
            Session session = sessionFactory.getCurrentSession();
            Preconditions.checkNotNull(session);
            Preconditions.checkArgument(session.isConnected());
            return session;
        } catch (PersistenceException e) {
            throw new TransactionException(e);
        }
    }

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    public DataStoreTransaction beginTransaction() {
        Session session = sessionFactory.getCurrentSession();
        Preconditions.checkNotNull(session);
        session.beginTransaction();
        return transactionSupplier.get(session, isScrollEnabled, scrollMode);
    }
}
