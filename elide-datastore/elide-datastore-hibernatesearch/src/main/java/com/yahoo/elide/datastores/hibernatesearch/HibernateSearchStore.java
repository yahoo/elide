/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernatesearch;

import com.google.common.base.Preconditions;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;


/**
 * Hibernate interface library.
 */
public class HibernateSearchStore extends HibernateStore {
    private final SessionFactory sessionFactory;

    /**
     * Initialize HibernateStore and dictionaries.
     *
     * @param aSessionFactory the a session factory
     */
    public HibernateSearchStore(SessionFactory aSessionFactory) {
        super(aSessionFactory);
        this.sessionFactory = aSessionFactory;
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
        return new HibernateSearchTransaction(session, true, ScrollMode.FORWARD_ONLY);
    }
}
