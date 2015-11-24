/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.google.common.base.Preconditions;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.security.User;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import java.io.IOException;
import java.io.Serializable;

/**
 * Manager for javax.persistence compatible db resource.
 */
public class PersistenceStore implements DataStore {
    private final EntityManagerFactory entityManagerFactory;

    public PersistenceStore(EntityManagerFactory entityManagerFactory) {
        Preconditions.checkNotNull(entityManagerFactory);
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (EntityType entity : entityManagerFactory.getMetamodel().getEntities()) {
            dictionary.bindEntity(entity.getBindableJavaType());
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new PersistenceTransaction(entityManagerFactory.createEntityManager());
    }

    /**
     * The type Persistence transaction.
     */
    public class PersistenceTransaction implements DataStoreTransaction {
        private final EntityManager entityManager;

        public PersistenceTransaction(EntityManager entityManager) {
            this.entityManager = entityManager;
            this.entityManager.getTransaction().begin();
        }

        @Override
        public void save(Object entity) {
            entityManager.persist(entity);
        }

        @Override
        public void delete(Object entity) {
            entityManager.remove(entity);
        }

        @Override
        public void flush() {
            entityManager.flush();
        }

        @Override
        public void commit() {
            flush();
            entityManager.getTransaction().commit();
        }

        @Override
        public <T> T createObject(Class<T> entityClass) {
            try {
                T entity = entityClass.newInstance();
                entityManager.persist(entity);
                return entity;
            } catch (InstantiationException | IllegalAccessException e) {
                return null;
            }
        }

        @Override
        public <T> T loadObject(Class<T> entityClass, Serializable id) {
            return entityManager.find(entityClass, id);
        }

        @Override
        public <T> Iterable<T> loadObjects(Class<T> entityClass) {
            return entityManager.createQuery("from " + entityClass.getName(), entityClass).getResultList();
        }

        @Override
        public void close() throws IOException {
            try {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                    throw new IOException("Transaction not closed");
                }
            } finally {
                entityManager.close();
            }
        }

        @Override
        public User accessUser(Object opaqueUser) {
            return new User(opaqueUser);
        }
    }
}
