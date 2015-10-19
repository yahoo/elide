/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.dbmanagers.hibernate3;

import com.yahoo.elide.core.DatabaseManager;
import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.CriteriaCheck;
import com.yahoo.elide.security.User;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import lombok.NonNull;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;

import java.io.IOException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Hibernate interface library.
 */
public class HibernateManager extends DatabaseManager {

    /**
     * Wraps ScrollableResult as Iterator.
     *
     * @param <T> type of return object
     */
    public static class ScrollableIterator<T> implements Iterable<T>, Iterator<T> {

        final private ScrollableResults scroll;
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
     * Hibernate Transaction implementation.
     */
    public class HibernateTransaction implements DatabaseTransaction {

        private final Transaction transaction;
        private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();

        /**
         * Instantiates a new Hibernate transaction.
         *
         * @param transaction the transaction
         */
        public HibernateTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public void delete(Object object) {
            deferredTasks.add(() -> getSession().delete(object));
        }

        @Override
        public void save(Object object) {
            deferredTasks.add(() -> getSession().saveOrUpdate(object));
        }

        @Override
        public void flush() {
            try {
                deferredTasks.forEach(Runnable::run);
                deferredTasks.clear();
                getSession().flush();
            } catch (HibernateException e) {
                throw new TransactionException(e);
            }
        }

        @Override
        public void commit() {
            try {
                this.flush();
                this.transaction.commit();
            } catch (HibernateException e) {
                throw new TransactionException(e);
            }
        }

        @Override
        public <T> T createObject(Class<T> entityClass) {
            try {
                T object = entityClass.newInstance();
                deferredTasks.add(() -> getSession().persist(object));
                return object;
            } catch (InstantiationException | IllegalAccessException e) {
                return null;
            }
        }

        @Override
        public <T> T loadObject(Class<T> loadClass, String id) {
            @SuppressWarnings("unchecked")
            T record = (T) getSession().load(loadClass, Long.valueOf(id));
            try {
                Hibernate.initialize(record);
            } catch (ObjectNotFoundException e) {
                return null;
            }
            return record;
        }

        @Override
        public <T> Iterable<T> loadObjects(Class<T> loadClass) {
            @SuppressWarnings("unchecked")
            Iterable<T> list = new ScrollableIterator(getSession().createCriteria(loadClass)
                    .scroll(ScrollMode.FORWARD_ONLY));
            return list;
        }
        @Override
        public <T> Iterable<T> loadObjects(Class<T> loadClass, FilterScope<T> filterScope) {
            Criterion criterion = buildCriterion(filterScope);

            // if no criterion then return all objects
            if (criterion == null) {
                return loadObjects(loadClass);
            }

            @SuppressWarnings("unchecked")
            Iterable<T> list = new ScrollableIterator(getSession().createCriteria(loadClass)
                    .add(criterion)
                    .scroll(ScrollMode.FORWARD_ONLY));
            return list;
        }

        /**
         * builds criterion if all checks implement CriteriaCheck.
         *
         * @param filterScope the filterScope
         * @return the criterion
         */
        public <T> Criterion buildCriterion(FilterScope<T> filterScope) {
            Criterion compositeCriterion = null;
            List<Check<T>> checks = filterScope.getChecks();
            RequestScope requestScope = filterScope.getRequestScope();
            for (Check check : checks) {
                         Criterion criterion;
                if (check instanceof CriteriaCheck) {
                    criterion = ((CriteriaCheck) check).getCriterion(requestScope);
                } else {
                    criterion = null;
                }

                // if no criterion, examine userPermission and ANY state
                if (criterion == null) {
                    switch (filterScope.getRequestScope().getUser().checkUserPermission(check)) {
                        // ALLOW and ALL try more criteria
                        case ALLOW:
                            if (!filterScope.isAny()) {
                                continue;
                            }
                            break;

                        // DENY and ANY check try more criteria
                        case DENY:
                            if (filterScope.isAny()) {
                                continue;
                            }
                            break;
                    }

                    // Otherwise no criteria filtering possible
                    return null;
                } else  if (compositeCriterion == null) {
                    compositeCriterion = criterion;
                } else if (filterScope.isAny()) {
                    compositeCriterion = Restrictions.or(compositeCriterion, criterion);
                } else {
                    compositeCriterion = Restrictions.and(compositeCriterion, criterion);
                }
            }
            return compositeCriterion;
        }

        @Override
        public void close() throws IOException {
            if (this.transaction.isActive()) {
                transaction.rollback();
                throw new IOException("Transaction not closed");
            }
        }

        @Override
        public User accessUser(Object opaqueUser) {
            return new User(opaqueUser);
        }
    }

    private final SessionFactory sessionFactory;

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
        Session session = sessionFactory.getCurrentSession();
        Preconditions.checkNotNull(session);
        Preconditions.checkArgument(session.isConnected());
        return session;
    }

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    public DatabaseTransaction beginTransaction() {
        Session session = sessionFactory.getCurrentSession();
        Preconditions.checkNotNull(session);
        return new HibernateTransaction(session.beginTransaction());
    }
}
