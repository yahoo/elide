/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.User;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.EntityManager;

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
    public void createObject(Object entity, RequestScope scope) {
        entityManager.persist(entity);
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
