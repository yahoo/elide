/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.dbmanagers.hibernate3;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.yahoo.elide.core.DatabaseManager;
import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import lombok.NonNull;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * HibernateManager for Hibernate 3.
 */
public class HibernateManager implements DatabaseManager {
    private final SessionFactory sessionFactory;

    /**
     * Wraps ScrollableResult as Iterator.
     *
     * @param <T> type of return object
     */
    public static class ScrollableIterator<T> implements Iterable<T>, Iterator<T> {
        private final ScrollableResults scroll;
        private boolean inUse = false;
        private boolean hasNext;

        public ScrollableIterator(ScrollableResults scroll) {
            this.scroll = scroll;
        }

        @Override
        public Iterator<T> iterator() {
            if (inUse) {
                throw new ConcurrentModificationException();
            }

            if (!scroll.first()) {
                return Collections.emptyListIterator();
            }

            inUse = true;
            hasNext = true;
            return Iterators.unmodifiableIterator(this);
        }

        @Override
        public boolean hasNext() {
            return hasNext;

        }

        @Override
        public @NonNull T next() {
            @SuppressWarnings("unchecked")
            @NonNull T row = (T) scroll.get()[0];
            Preconditions.checkNotNull(row);
            hasNext = scroll.next();
            return row;
        }
    }

    /**
     * Initialize HibernateManager and dictionaries.
     *
     * @param aSessionFactory the a session factory
     */
    public HibernateManager(SessionFactory aSessionFactory) {
        this.sessionFactory = aSessionFactory;
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
     * Filters are enabled after starting transaction.
     *
     * @return transaction
     */
    @Override
    public DatabaseTransaction beginTransaction() {
        Session session = sessionFactory.getCurrentSession();
        Preconditions.checkNotNull(session);
        return new HibernateTransaction(this, session.beginTransaction());
    }

    @Override
    public DatabaseTransaction beginReadTransaction(MultivaluedMap<String, String> queryParams) {
        Session session = sessionFactory.getCurrentSession();
        Preconditions.checkNotNull(session);
        return new HibernateTransaction(this, session.beginTransaction(), queryParams);
    }
}
