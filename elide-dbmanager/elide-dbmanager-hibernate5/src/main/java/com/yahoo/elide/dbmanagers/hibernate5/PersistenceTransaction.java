package com.yahoo.elide.dbmanagers.hibernate5;

import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.security.User;

import javax.persistence.EntityManager;
import java.io.IOException;

/**
 * Created by danchen on 10/27/15.
 */
public class PersistenceTransaction implements DatabaseTransaction {
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
    public <T> T loadObject(Class<T> entityClass, String id) {
        return entityManager.find(entityClass, Long.valueOf(id));
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
