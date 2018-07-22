/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;

import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateEntityManager;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

/**
 * Hibernate5 store supporting the EntityManager.
 */
public class HibernateEntityManagerStore extends AbstractHibernateStore {
    protected final EntityManager entityManager;

    @Deprecated
    public HibernateEntityManagerStore(HibernateEntityManager entityManager,
                                       boolean isScrollEnabled,
                                       ScrollMode scrollMode) {
        super(null, isScrollEnabled, scrollMode);
        this.entityManager = entityManager;
    }

    public HibernateEntityManagerStore(EntityManager entityManager,
                                       boolean isScrollEnabled,
                                       ScrollMode scrollMode) {
        super(null, isScrollEnabled, scrollMode);
        this.entityManager = entityManager;
    }

    /**
     * Get current Hibernate session.
     *
     * @return session Hibernate session from EntityManager.
     */
    @Override
    public Session getSession() {
        return entityManager.unwrap(Session.class);
    }

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    @SuppressWarnings("resource")
    public DataStoreTransaction beginTransaction() {
        Session session = getSession();
        session.beginTransaction();
        session.clear();
        return transactionSupplier.get(session, isScrollEnabled, scrollMode);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (EntityType type : entityManager.getEntityManagerFactory().getMetamodel().getEntities()) {
            bindEntity(dictionary, type);
        }
    }
}
