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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;

/**
 * Hibernate5 store supporting the EntityManager.
 */
public class HibernateEntityManagerStore extends AbstractHibernateStore {
    protected final EntityManagerFactory entityManagerFactory;

    public HibernateEntityManagerStore(EntityManagerFactory entityManagerFactory,
                                       boolean isScrollEnabled,
                                       ScrollMode scrollMode) {
        super(null, isScrollEnabled, scrollMode);
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    @SuppressWarnings("resource")
    public DataStoreTransaction beginTransaction() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        Session session = manager.unwrap(Session.class);
        session.beginTransaction();
        session.clear();
        return transactionSupplier.get(session, isScrollEnabled, scrollMode);
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (EntityType<?> type : entityManagerFactory.getMetamodel().getEntities()) {
            bindEntity(dictionary, type);
        }
    }
}
