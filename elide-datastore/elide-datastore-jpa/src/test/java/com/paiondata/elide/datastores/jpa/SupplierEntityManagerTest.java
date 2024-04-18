/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpa;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies the SupplierEntityManager delegates.
 */
class SupplierEntityManagerTest {

    EntityManager targetEntityManager;

    SupplierEntityManager entityManager;

    @BeforeEach
    void setup() {
        this.targetEntityManager = mock(EntityManager.class);
        this.entityManager = new SupplierEntityManager();
        this.entityManager.setEntityManager(this.targetEntityManager);
    }

    @Test
    void isOpenDefault() {
        try (SupplierEntityManager supplierEntityManager = new SupplierEntityManager()) {
            assertFalse(supplierEntityManager.isOpen());
        }
    }

    @Test
    void persist() {
        Object entity = new Object();
        this.entityManager.persist(entity);
        verify(this.targetEntityManager).persist(entity);
    }

    @Test
    void merge() {
        Object entity = new Object();
        this.entityManager.merge(entity);
        verify(this.targetEntityManager).merge(entity);
    }

    @Test
    void remove() {
        Object entity = new Object();
        this.entityManager.remove(entity);
        verify(this.targetEntityManager).remove(entity);
    }

    @Test
    void findClassObject() {
        Object entity = new Object();
        Object primaryKey = new Object();
        this.entityManager.find(entity.getClass(), primaryKey);
        verify(this.targetEntityManager).find(entity.getClass(), primaryKey);
    }

    @Test
    void findClassObjectMap() {
        Object entity = new Object();
        Object primaryKey = new Object();
        Map<String, Object> properties = new HashMap<>();
        this.entityManager.find(entity.getClass(), primaryKey, properties);
        verify(this.targetEntityManager).find(entity.getClass(), primaryKey, properties);
    }

    @Test
    void findClassObjectLockModeType() {
        Object entity = new Object();
        Object primaryKey = new Object();
        LockModeType lockMode = LockModeType.NONE;
        this.entityManager.find(entity.getClass(), primaryKey, lockMode);
        verify(this.targetEntityManager).find(entity.getClass(), primaryKey, lockMode);
    }

    @Test
    void findClassObjectLockModeTypeMap() {
        Object entity = new Object();
        Object primaryKey = new Object();
        LockModeType lockMode = LockModeType.NONE;
        Map<String, Object> properties = new HashMap<>();
        this.entityManager.find(entity.getClass(), primaryKey, lockMode, properties);
        verify(this.targetEntityManager).find(entity.getClass(), primaryKey, lockMode, properties);
    }

    @Test
    void getReference() {
        Object entity = new Object();
        Object primaryKey = new Object();
        this.entityManager.getReference(entity.getClass(), primaryKey);
        verify(this.targetEntityManager).getReference(entity.getClass(), primaryKey);
    }

    @Test
    void flush() {
        this.entityManager.flush();
        verify(this.targetEntityManager).flush();
    }

    @Test
    void setFlushMode() {
        FlushModeType flushMode = FlushModeType.COMMIT;
        this.entityManager.setFlushMode(flushMode);
        verify(this.targetEntityManager).setFlushMode(flushMode);
    }

    @Test
    void getFlushMode() {
        FlushModeType flushMode = FlushModeType.COMMIT;
        when(this.targetEntityManager.getFlushMode()).thenReturn(flushMode);
        assertEquals(flushMode, this.entityManager.getFlushMode());
    }

    @Test
    void lockObjectLockMode() {
        Object entity = new Object();
        LockModeType lockMode = LockModeType.NONE;
        this.entityManager.lock(entity, lockMode);
        verify(this.targetEntityManager).lock(entity, lockMode);
    }

    @Test
    void lockObjectLockModeTypeMap() {
        Object entity = new Object();
        LockModeType lockMode = LockModeType.NONE;
        Map<String, Object> properties = new HashMap<>();
        this.entityManager.lock(entity, lockMode, properties);
        verify(this.targetEntityManager).lock(entity, lockMode, properties);
    }

    @Test
    void refreshObject() {
        Object entity = new Object();
        this.entityManager.refresh(entity);
        verify(this.targetEntityManager).refresh(entity);
    }

    @Test
    void refreshObjectMap() {
        Object entity = new Object();
        Map<String, Object> properties = new HashMap<>();
        this.entityManager.refresh(entity, properties);
        verify(this.targetEntityManager).refresh(entity, properties);
    }

    @Test
    void refreshObjectLockModeType() {
        Object entity = new Object();
        LockModeType lockMode = LockModeType.NONE;
        this.entityManager.refresh(entity, lockMode);
        verify(this.targetEntityManager).refresh(entity, lockMode);
    }

    @Test
    void refreshObjectLockModeTypeMap() {
        Object entity = new Object();
        LockModeType lockMode = LockModeType.NONE;
        Map<String, Object> properties = new HashMap<>();
        this.entityManager.refresh(entity, lockMode, properties);
        verify(this.targetEntityManager).refresh(entity, lockMode, properties);
    }

    @Test
    void clear() {
        this.entityManager.clear();
        verify(this.targetEntityManager).clear();
    }

    @Test
    void detach() {
        Object entity = new Object();
        this.entityManager.detach(entity);
        verify(this.targetEntityManager).detach(entity);
    }

    @Test
    void contains() {
        Object entity = new Object();
        when(this.targetEntityManager.contains(entity)).thenReturn(true);
        assertTrue(this.entityManager.contains(entity));
    }

    @Test
    void getLockMode() {
        Object entity = new Object();
        when(this.targetEntityManager.getLockMode(entity)).thenReturn(LockModeType.NONE);
        assertEquals(LockModeType.NONE, this.entityManager.getLockMode(entity));
    }

    @Test
    void setProperty() {
        Object value = new Object();
        this.entityManager.setProperty("propertyName", value);
        verify(this.targetEntityManager).setProperty("propertyName", value);

    }

    @Test
    void getProperties() {
        Map<String, Object> properties = new HashMap<>();
        when(this.targetEntityManager.getProperties()).thenReturn(properties);
        assertEquals(properties, this.entityManager.getProperties());
    }

    @Test
    void createQueryString() {
        String qlString = "";
        this.entityManager.createQuery(qlString);
        verify(this.targetEntityManager).createQuery(qlString);
    }

    @Test
    void createQueryCriteriaQuery() {
        CriteriaQuery<?> criteriaQuery = null;
        this.entityManager.createQuery(criteriaQuery);
        verify(this.targetEntityManager).createQuery(criteriaQuery);
    }

    @Test
    void createQueryCriteriaUpdate() {
        CriteriaUpdate<?> updateQuery = null;
        this.entityManager.createQuery(updateQuery);
        verify(this.targetEntityManager).createQuery(updateQuery);
    }

    @Test
    void createQueryCriteriaDelete() {
        CriteriaDelete<?> deleteQuery = null;
        this.entityManager.createQuery(deleteQuery);
        verify(this.targetEntityManager).createQuery(deleteQuery);
    }

    @Test
    void createQueryStringClass() {
        String qlString = "";
        String result = "";
        this.entityManager.createQuery(qlString, result.getClass());
        verify(this.targetEntityManager).createQuery(qlString, result.getClass());
    }

    @Test
    void createNamedQueryString() {
        this.entityManager.createNamedQuery("name");
        verify(this.targetEntityManager).createNamedQuery("name");
    }

    @Test
    void createNamedQueryStringClass() {
        String name = "";
        String result = "";
        this.entityManager.createNamedQuery(name, result.getClass());
        verify(this.targetEntityManager).createNamedQuery(name, result.getClass());
    }

    @Test
    void createNativeQueryString() {
        String sqlString = "";
        this.entityManager.createNativeQuery(sqlString);
        verify(this.targetEntityManager).createNativeQuery(sqlString);
    }

    @Test
    void createNativeQueryStringClass() {
        String sqlString = "";
        String result = "";
        this.entityManager.createNativeQuery(sqlString, result.getClass());
        verify(this.targetEntityManager).createNativeQuery(sqlString, result.getClass());
    }

    @Test
    void createNativeQueryStringString() {
        String sqlString = "";
        String resultSetMapping = "";
        this.entityManager.createNativeQuery(sqlString, resultSetMapping);
        verify(this.targetEntityManager).createNativeQuery(sqlString, resultSetMapping);
    }

    @Test
    void createNamedStoredProcedureQuery() {
        String name = "";
        this.entityManager.createNamedStoredProcedureQuery(name);
        verify(this.targetEntityManager).createNamedStoredProcedureQuery(name);
    }

    @Test
    void createStoredProcedureQueryString() {
        String procedureName = "";
        this.entityManager.createStoredProcedureQuery(procedureName);
        verify(this.targetEntityManager).createStoredProcedureQuery(procedureName);
    }

    @Test
    void createStoredProcedureQueryStringClass() {
        String procedureName = "";
        Class<?>[] resultClasses = new Class[] { String.class };
        this.entityManager.createStoredProcedureQuery(procedureName, resultClasses);
        verify(this.targetEntityManager).createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Test
    void createStoredProcedureQueryStringString() {
        String procedureName = "";
        String[] resultSetMappings = new String[] { "" };
        this.entityManager.createStoredProcedureQuery(procedureName, resultSetMappings);
        verify(this.targetEntityManager).createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Test
    void joinTransaction() {
        this.entityManager.joinTransaction();
        verify(this.targetEntityManager).joinTransaction();
    }

    @Test
    void isJoinedToTransaction() {
        this.entityManager.isJoinedToTransaction();
        verify(this.targetEntityManager).isJoinedToTransaction();
    }

    @Test
    void unwrap() {
        Class<?> cls = null;
        this.entityManager.unwrap(cls);
        verify(this.targetEntityManager).unwrap(cls);
    }

    @Test
    void getDelegate() {
        this.entityManager.getDelegate();
        verify(this.targetEntityManager).getDelegate();
    }

    @Test
    void close() {
        this.entityManager.close();
        verify(this.targetEntityManager).close();
    }

    @Test
    void isOpen() {
        this.entityManager.isOpen();
        verify(this.targetEntityManager).isOpen();
    }

    @Test
    void getTransaction() {
        this.entityManager.getTransaction();
        verify(this.targetEntityManager).getTransaction();
    }

    @Test
    void getEntityManagerFactory() {
        this.entityManager.getEntityManagerFactory();
        verify(this.targetEntityManager).getEntityManagerFactory();
    }

    @Test
    void getCriteriaBuilder() {
        this.entityManager.getCriteriaBuilder();
        verify(this.targetEntityManager).getCriteriaBuilder();
    }

    @Test
    void getMetamodel() {
        this.entityManager.getMetamodel();
        verify(this.targetEntityManager).getMetamodel();
    }

    @Test
    void createEntityGraphClass() {
        Class<?> rootType = null;
        this.entityManager.createEntityGraph(rootType);
        verify(this.targetEntityManager).createEntityGraph(rootType);
    }

    @Test
    void createEntityGraphString() {
        String graphName = "";
        this.entityManager.createEntityGraph(graphName);
        verify(this.targetEntityManager).createEntityGraph(graphName);
    }

    @Test
    void getEntityGraph() {
        String graphName = "";
        this.entityManager.getEntityGraph(graphName);
        verify(this.targetEntityManager).getEntityGraph(graphName);
    }

    @Test
    void getEntityGraphs() {
        Class<?> entityClass = null;
        this.entityManager.getEntityGraphs(entityClass);
        verify(this.targetEntityManager).getEntityGraphs(entityClass);
    }
}
