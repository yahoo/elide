/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.integration.tests;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;

/**
 * Data Store that wraps another store and provides delay for testing Async queries.
 */
public class AsyncDelayDataStore implements DataStore {

    private DataStore delayStore;

    public AsyncDelayDataStore(DataStore delayStore, Integer testDelay) {
        this.delayStore = delayStore;
    }

    public AsyncDelayDataStore(DataStore delayStore) {
        this.delayStore = delayStore;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        delayStore.populateEntityDictionary(dictionary);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AsyncDelayStoreTransaction(delayStore.beginTransaction());
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new AsyncDelayStoreTransaction(delayStore.beginReadTransaction());
    }
}
