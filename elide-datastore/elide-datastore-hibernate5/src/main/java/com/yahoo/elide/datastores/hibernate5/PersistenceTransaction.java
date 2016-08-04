/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

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
    public void save(Object entity, RequestScope requestScope) {
        entityManager.persist(entity);
    }

    @Override
    public void delete(Object entity, RequestScope requestScope) {
        entityManager.remove(entity);
    }

    @Override
    public void flush(RequestScope requestScope) {
        entityManager.flush();
    }

    @Override
    public void commit(RequestScope requestScope) {
        flush(requestScope);
        entityManager.getTransaction().commit();
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        entityManager.persist(entity);
    }

    @Override
    public Object loadObject(Class<?> entityClass,
                      Serializable id,
                      Optional<FilterExpression> filterExpression,
                      RequestScope scope) {
        return entityManager.find(entityClass, id);
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass,
                                        Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting, Optional<Pagination> pagination,
                                        RequestScope scope) {
        return (Iterable<Object>) entityManager.createQuery("from " + entityClass.getName(), entityClass)
                .getResultList();
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity, String relationName,
                              Optional<FilterExpression> filterExpression,
                              Optional<Sorting> sorting,
                              Optional<Pagination> pagination,
                              RequestScope scope) {
        return null;
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
