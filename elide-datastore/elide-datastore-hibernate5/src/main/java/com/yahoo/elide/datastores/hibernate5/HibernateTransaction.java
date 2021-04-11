/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.hibernate.JPQLTransaction;
import com.yahoo.elide.datastores.hibernate5.porting.SessionWrapper;

import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.internal.AbstractPersistentCollection;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;

import javax.persistence.PersistenceException;

/**
 * Hibernate Transaction implementation.
 */
@Slf4j
public class HibernateTransaction extends JPQLTransaction {

    private final Session session;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final boolean isScrollEnabled;
    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param isScrollEnabled Whether or not scrolling is enabled
     * @param scrollMode Scroll mode to use if scrolling enabled
     */
    protected HibernateTransaction(Session session, boolean isScrollEnabled, ScrollMode scrollMode) {
        super(new SessionWrapper(session), isScrollEnabled);
        this.session = session;
        // Elide must not flush until all beans are ready
        FlushMode flushMode = session.getHibernateFlushMode();
        if (flushMode != FlushMode.COMMIT && flushMode != FlushMode.MANUAL) {
            session.setHibernateFlushMode(FlushMode.COMMIT);
        }
        this.isScrollEnabled = isScrollEnabled;
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
        } catch (PersistenceException e) {
            log.error("Caught hibernate exception during flush", e);
            throw new TransactionException(e);
        }
    }

    protected void hibernateFlush(RequestScope requestScope) {
        FlushMode flushMode = session.getHibernateFlushMode();
        if (flushMode != FlushMode.MANUAL) {
            session.flush();
        }
    }

    @Override
    public void commit(RequestScope scope) {
        try {
            this.flush(scope);
            this.session.getTransaction().commit();
        } catch (PersistenceException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        deferredTasks.add(() -> session.persist(entity));
    }

    @Override
    public void close() throws IOException {
        if (session.isOpen() && session.getTransaction().getStatus().canRollback()) {
            session.getTransaction().rollback();
            throw new IOException("Transaction not closed");
        }
    }

    @Override
    public void cancel(RequestScope scope) {
        session.cancelQuery();
    }


    @Override
    protected boolean isAbstractCollection(Collection<?> collection) {
        return collection instanceof AbstractPersistentCollection;
    }
}
