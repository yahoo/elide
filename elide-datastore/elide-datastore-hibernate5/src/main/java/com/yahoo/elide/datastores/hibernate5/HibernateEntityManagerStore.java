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
import org.hibernate.metadata.ClassMetadata;

/**
 * Hibernate5 store supporting the EntityManager.
 */
public class HibernateEntityManagerStore extends HibernateStore {
    protected final HibernateEntityManager entityManager;

    public HibernateEntityManagerStore(HibernateEntityManager entityManager,
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
    @SuppressWarnings("deprecation")
    public Session getSession() {
        return entityManager.getSession();
    }

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    @SuppressWarnings("deprecation")
    public DataStoreTransaction beginTransaction() {
        Session session = getSession();
        session.beginTransaction();
        return transactionSupplier.get(session, isScrollEnabled, scrollMode);
    }

    /**
     * Populate the entity dictionary from hibernate entities.
     *
     * @param dictionary Entity dictionary to populate
     */
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (ClassMetadata meta : getSession().getSessionFactory().getAllClassMetadata().values()) {
            dictionary.bindEntity(meta.getMappedClass());
        }
    }
}
