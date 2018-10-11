/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManager;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

/**
 * Hibernate interface library.
 */
public abstract class AbstractHibernateStore implements DataStore {
    protected final SessionFactory sessionFactory;
    protected final boolean isScrollEnabled;
    protected final ScrollMode scrollMode;
    protected final HibernateTransactionSupplier transactionSupplier;

    /**
     * Constructor.
     *
     * @param aSessionFactory Session factory
     * @param isScrollEnabled Whether or not scrolling is enabled on driver
     * @param scrollMode Scroll mode to use for scrolling driver
     */
    protected AbstractHibernateStore(SessionFactory aSessionFactory, boolean isScrollEnabled, ScrollMode scrollMode) {
        this(aSessionFactory, isScrollEnabled, scrollMode, HibernateTransaction::new);
    }

    /**
     * Constructor.
     *
     * Useful for extending the store and relying on existing code
     * to instantiate custom hibernate transaction.
     *
     * @param aSessionFactory Session factory
     * @param isScrollEnabled Whether or not scrolling is enabled on driver
     * @param scrollMode Scroll mode to use for scrolling driver
     * @param transactionSupplier Supplier for transaction
     */
    protected AbstractHibernateStore(SessionFactory aSessionFactory,
                                     boolean isScrollEnabled,
                                     ScrollMode scrollMode,
                                     HibernateTransactionSupplier transactionSupplier) {
        this.sessionFactory = aSessionFactory;
        this.isScrollEnabled = isScrollEnabled;
        this.scrollMode = scrollMode;
        this.transactionSupplier = transactionSupplier;
    }

    /**
     * Builder object to configuration hibernate store.
     */
    public static class Builder {
        private final SessionFactory sessionFactory;
        private final EntityManager entityManager;
        private boolean isScrollEnabled;
        private ScrollMode scrollMode;

        public Builder(final SessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
            this.entityManager = null;
        }

        public Builder(final EntityManager entityManager) {
            this.sessionFactory = null;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
            this.entityManager = entityManager;
        }

        @Deprecated
        public Builder(final HibernateEntityManager entityManager) {
            this.sessionFactory = null;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
            this.entityManager = entityManager;
        }

        public Builder withScrollEnabled(final boolean isScrollEnabled) {
            this.isScrollEnabled = isScrollEnabled;
            return this;
        }

        public Builder withScrollMode(final ScrollMode scrollMode) {
            this.scrollMode = scrollMode;
            return this;
        }

        public AbstractHibernateStore build() {
            if (sessionFactory != null) {
                return new HibernateSessionFactoryStore(sessionFactory, isScrollEnabled, scrollMode);
            } else if (entityManager != null) {
                return new HibernateEntityManagerStore(entityManager, isScrollEnabled, scrollMode);
            }
            throw new IllegalStateException("Either an EntityManager or SessionFactory is required!");
        }
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (EntityType type : sessionFactory.getMetamodel().getEntities()) {
            bindEntity(dictionary, type);
        }
    }

    protected void bindEntity(EntityDictionary dictionary, EntityType type) {
        try {
            Class mappedClass = type.getJavaType();
            // Ignore this result. We are just checking to see if it throws an exception meaning that
            // provided class was _not_ an entity.
            dictionary.lookupEntityClass(mappedClass);

            // Bind if successful
            dictionary.bindEntity(mappedClass);
        } catch (IllegalArgumentException e)  {
            // Ignore this entity
            // Turns out that hibernate may include non-entity types in this list when using things
            // like envers. Since they are not entities, we do not want to bind them into the entity
            // dictionary
        }
    }

    /**
     * Get current Hibernate session.
     *
     * @return session
     */
    abstract public Session getSession();

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    abstract public DataStoreTransaction beginTransaction();

    /**
     * Functional interface for describing a method to supply a custom Hibernate transaction.
     */
    @FunctionalInterface
    public interface HibernateTransactionSupplier {
        HibernateTransaction get(Session session, boolean isScrollEnabled, ScrollMode scrollMode);
    }
}
