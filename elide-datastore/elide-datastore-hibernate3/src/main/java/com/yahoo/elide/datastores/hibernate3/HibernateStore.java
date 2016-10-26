/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;

import com.google.common.base.Preconditions;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

/**
 * Hibernate interface library.
 */
public class HibernateStore implements DataStore {
    private final SessionFactory sessionFactory;
    private final boolean isScrollEnabled;
    private final ScrollMode scrollMode;

    /**
     * Constructor.
     *
     * @param aSessionFactory Session factory
     * @param isScrollEnabled Whether or not scrolling is enabled on driver
     * @param scrollMode Scroll mode to use for scrolling driver
     */
    protected HibernateStore(SessionFactory aSessionFactory, boolean isScrollEnabled, ScrollMode scrollMode) {
        this.sessionFactory = aSessionFactory;
        this.isScrollEnabled = isScrollEnabled;
        this.scrollMode = scrollMode;
    }

    /**
     * Builder object to configuration hibernate transaction.
     */
    public static class Builder {
        private final SessionFactory sessionFactory;
        private boolean isScrollEnabled;
        private ScrollMode scrollMode;

        public Builder(final SessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
        }

        public Builder withScrollEnabled(final boolean isScrollEnabled) {
            this.isScrollEnabled = isScrollEnabled;
            return this;
        }

        public Builder withScrollMode(final ScrollMode scrollMode) {
            this.scrollMode = scrollMode;
            return this;
        }

        public HibernateStore build() {
            return new HibernateStore(sessionFactory, isScrollEnabled, scrollMode);
        }
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (ClassMetadata meta : sessionFactory.getAllClassMetadata().values()) {
            dictionary.bindEntity(meta.getMappedClass(EntityMode.POJO));
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
        session.beginTransaction();
        return new HibernateTransaction(session, isScrollEnabled, scrollMode);
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        Session session = sessionFactory.getCurrentSession();
        Preconditions.checkNotNull(session);
        session.beginTransaction();
        session.setDefaultReadOnly(true);
        return new HibernateTransaction(session, isScrollEnabled, scrollMode);
    }
}
