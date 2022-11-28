/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.EntityProjection;

import jakarta.persistence.Entity;

import java.io.IOException;
import java.io.Serializable;

class TestDataStore implements DataStore, DataStoreTransaction {

    private final Package beanPackage;

    public TestDataStore(Package beanPackage) {
        this.beanPackage = beanPackage;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        dictionary.getScanner().getAnnotatedClasses(beanPackage, Entity.class).stream().forEach(dictionary::bindEntity);
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
    public DataStoreIterable<Object> loadObjects(EntityProjection projection, RequestScope scope) {
        throw new TransactionException(null);
    }

    @Override
    public void cancel(RequestScope scope) {
        // Nothing
    }
}
