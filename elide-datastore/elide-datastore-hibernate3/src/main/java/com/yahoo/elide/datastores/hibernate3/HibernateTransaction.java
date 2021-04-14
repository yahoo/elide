/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.hibernate.JPQLTransaction;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.datastores.hibernate3.porting.SessionWrapper;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.AbstractPersistentCollection;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Predicate;

import javax.persistence.NoResultException;


/**
 * Hibernate Transaction implementation.
 */
@Slf4j
public class HibernateTransaction extends JPQLTransaction {

    private final Session session;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();

    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param isScrollEnabled Whether or not scrolling is enabled
     * @param scrollMode Scroll mode to use if scrolling enabled
     */
    protected HibernateTransaction(Session session, boolean delegateToInMemoryStore, boolean isScrollEnabled,
            ScrollMode scrollMode) {
        super(new SessionWrapper(session), delegateToInMemoryStore, isScrollEnabled);
        this.session = session;
    }

    protected HibernateTransaction(Session session, boolean isScrollEnabled, ScrollMode scrollMode) {
        this(session, true, isScrollEnabled, scrollMode);
    }

    @Override
    public <T> void delete(T object, RequestScope scope) {
        deferredTasks.add(() -> session.delete(object));
    }

    @Override
    public <T> void save(T object, RequestScope scope) {
        deferredTasks.add(() -> session.saveOrUpdate(object));
    }

    @Override
    public void flush(RequestScope requestScope) {
        try {
            deferredTasks.forEach(Runnable::run);
            deferredTasks.clear();
            hibernateFlush(requestScope);
        } catch (HibernateException e) {
            log.error("Caught hibernate exception during flush", e);
            throw new TransactionException(e);
        }
    }

    protected void hibernateFlush(RequestScope requestScope) {
        FlushMode flushMode = session.getFlushMode();
        if (flushMode != FlushMode.COMMIT && flushMode != FlushMode.MANUAL && flushMode != FlushMode.NEVER) {
            session.flush();
        }
    }

    @Override
    public void commit(RequestScope scope) {
        try {
            this.flush(scope);
            this.session.getTransaction().commit();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        deferredTasks.add(() -> session.persist(entity));
    }

    @Override
    public <T> T loadObject(EntityProjection projection,
            Serializable id,
            RequestScope scope) {

        try {
            return super.loadObject(projection, id, scope);
        } catch (ObjectNotFoundException | NoResultException e) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        if (session.isOpen() && session.getTransaction().isActive()) {
            session.getTransaction().rollback();
            throw new IOException("Transaction not closed");
        }
    }

    /**
     * Overrideable default query limit for the data store.
     *
     * @return default limit
     */
    public Integer getQueryLimit() {
        // no limit
        return null;
    }

    @Override
    public void cancel(RequestScope scope) {
        session.cancelQuery();
    }

    @Override
    protected Predicate<Collection<?>> isPersistentCollection() {
        return AbstractPersistentCollection.class::isInstance;
    }
}
