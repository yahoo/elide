/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.kundera;

import com.google.common.base.Preconditions;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

import javax.persistence.Persistence;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Kundera interface library.
 */
public class KunderaStore implements DataStore {
    private final EntityManagerFactory entityManagerFactory;

    /**
     * Constructor
     *
     * @param entityManagerFactory Entity Manager Factory
     */
    public KunderaStore(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public static class Builder {
        private final EntityManagerFactory entityManagerFactory;

        public Builder(EntityManagerFactory entityManagerFactory) {
            this.entityManagerFactory = entityManagerFactory;
        }

        public KunderaStore build() {
            return new KunderaStore(entityManagerFactory);
        }
    }

    /**
     * Load entity dictionary with JPA annotated beans.
     *
     * @param dictionary the dictionary
     */
    public void populateEntityDictionary(EntityDictionary dictionary) {
    }

    /**
     * Begin Kundera transaction.
     *
     * @return transaction
     */
    @Override
    public DataStoreTransaction beginTransaction() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Preconditions.checkNotNull(entityManager);
        return new KunderaTransaction(entityManager);
    }
}
