/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.utils.ClassScanner;

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
        ClassScanner.getAnnotatedClasses(beanPackage, Entity.class).stream().forEach(dictionary::bindEntity);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return this;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void save(Object entity, RequestScope scope) {
    }

    @Override
    public void delete(Object entity, RequestScope scope) {
        throw new UnsupportedOperationException(this.toString());
    }

    @Override
    public void flush(RequestScope scope) {
        // Nothing
    }

    @Override
    public void commit(RequestScope scope) {
        throw new UnsupportedOperationException(this.toString());
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {

    }

    @Override
    public Object loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {
        throw new TransactionException(null);
    }

    @Override
    public Iterable<Object> loadObjects(
            EntityProjection projection,
            RequestScope scope) {
        throw new TransactionException(null);
    }

    @Override
    public void cancel(RequestScope scope) {
        // Nothing
    }
}
