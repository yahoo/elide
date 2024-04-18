/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.datastore.inmemory;

import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;

/**
 * Data Store that wraps another store and provides in-memory filtering, soring, and pagination
 * when the underlying store cannot perform the equivalent function.
 */
public class InMemoryDataStore implements DataStore {

    private DataStore wrappedStore;

    public InMemoryDataStore(DataStore wrappedStore) {
        this.wrappedStore = wrappedStore;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        wrappedStore.populateEntityDictionary(dictionary);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new InMemoryStoreTransaction(wrappedStore.beginTransaction());
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new InMemoryStoreTransaction(wrappedStore.beginReadTransaction());
    }

    @Override
    public String toString() {
        return "Wrapped:[" + String.valueOf(wrappedStore) + "]";
    }
}
