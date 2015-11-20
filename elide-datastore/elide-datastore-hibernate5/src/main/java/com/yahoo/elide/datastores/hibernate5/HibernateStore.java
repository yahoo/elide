/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.google.common.base.Preconditions;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

/**
 * Hibernate interface library.
 */
public class HibernateStore implements DataStore {
    private final SessionFactory sessionFactory;

    /**
     * Initialize HibernateStore and dictionaries.
     *
     * @param aSessionFactory the a session factory
     */
    public HibernateStore(SessionFactory aSessionFactory) {
        this.sessionFactory = aSessionFactory;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (ClassMetadata meta : sessionFactory.getAllClassMetadata().values()) {
            dictionary.bindEntity(meta.getMappedClass());
        }
    }

    /**
     * Get current Hibernate session.
     *
     * @return session
     */
    public Session getSession() {
        try {
            Session session = sessionFactory.getCurrentSession();
            Preconditions.checkNotNull(session);
            Preconditions.checkArgument(session.isConnected());
            return session;
        } catch (HibernateException e) {
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
        return new HibernateTransaction(session, session.beginTransaction());
    }
}
