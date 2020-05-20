/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.TransactionRegistry;

/**
 * Data Store that wraps another store and provides in-memory filtering, soring, and pagination
 * when the underlying store cannot perform the equivalent function.
 */

abstract class InMemoryTransaction implements TransactionRegistry {
    public void addRunningTransaction(TransactionEntry transactionEntry);
    public void removeRunningTransaction(TransactionEntry transactionEntry);
}

public class InMemoryDataStore extends InMemoryTransaction implements DataStore {

    private DataStore wrappedStore;
    public InMemoryDataStore(DataStore wrappedStore) {
        this.wrappedStore = wrappedStore;
    }

    @Deprecated
    public InMemoryDataStore(Package beanPackage) {
        this(new HashMapDataStore(beanPackage));
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        wrappedStore.populateEntityDictionary(dictionary);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        TransactionRegistry.TransactionEntry transactionEntry = new TransactionRegistry.TransactionEntry();
        InMemoryTransaction.addRunningTransaction(transactionEntry);
        return new InMemoryStoreTransaction(wrappedStore.beginTransaction());
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new InMemoryStoreTransaction(wrappedStore.beginReadTransaction());
    }
}
