/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Entity;

class TestDataStore implements DataStore, DataStoreTransaction {

    private final Package beanPackage;

    public TestDataStore(Package beanPackage) {
        this.beanPackage = beanPackage;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(beanPackage.getName()))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));
        for (Class<?> a : reflections.getTypesAnnotatedWith(Entity.class)) {
            if (a.getPackage().getName().startsWith(beanPackage.getName())) {
                dictionary.bindEntity(a);
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return this;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void save(Object entity) {
    }

    @Override
    public void delete(Object entity) {
        throw new UnsupportedOperationException("" + this);
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException("" + this);
    }

    @Override
    public <T> T createObject(Class<T> entityClass) {
        try {
            return entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException | Error | RuntimeException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public <T> T loadObject(Class<T> entityClass, Serializable id) {
        throw new TransactionException(null);
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> entityClass) {
        throw new TransactionException(null);
    }
}
