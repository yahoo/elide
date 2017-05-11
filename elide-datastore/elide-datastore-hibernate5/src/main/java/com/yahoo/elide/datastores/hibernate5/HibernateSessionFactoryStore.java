/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStoreTransaction;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Implementation for HibernateStore supporting SessionFactory.
 */
public class HibernateSessionFactoryStore extends HibernateStore {

    protected HibernateSessionFactoryStore(SessionFactory aSessionFactory,
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
    @SuppressWarnings("deprecation")
    public Session getSession() {
        // TODO: After removing, we should move logic from superclass to here
        return super.getSession();
    }

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    @SuppressWarnings("deprecation")
    public DataStoreTransaction beginTransaction() {
        // TODO: After removing, we should move logic from superclass to here
        return super.beginTransaction();
    }
}
