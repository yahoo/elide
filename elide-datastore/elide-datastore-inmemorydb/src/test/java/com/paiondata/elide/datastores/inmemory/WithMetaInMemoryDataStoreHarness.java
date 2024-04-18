/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.inmemory;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.core.datastore.wrapped.TransactionWrapper;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.example.beans.meta.Widget;

import java.util.Set;

/**
 * Harness that creates custom in memory store that sets metadata on the request scope.
 */
public class WithMetaInMemoryDataStoreHarness implements DataStoreTestHarness {

    HashMapDataStore wrappedStore = new HashMapDataStore(Set.of(Widget.class));

    @Override
    public DataStore getDataStore() {
        return new CustomStore();
    }

    @Override
    public void cleanseTestData() {
        wrappedStore.cleanseTestData();
    }

    class CustomTransaction extends TransactionWrapper {
        public CustomTransaction() {
            super(wrappedStore.beginTransaction());
        }

        @Override
        public <T> DataStoreIterable<T> loadObjects(EntityProjection projection, RequestScope scope) {
            scope.setMetadataField("foobar", 123);
            return super.loadObjects(projection, scope);
        }
    }

    class CustomStore implements DataStore {

        @Override
        public void populateEntityDictionary(EntityDictionary dictionary) {
            wrappedStore.populateEntityDictionary(dictionary);
        }

        @Override
        public DataStoreTransaction beginTransaction() {
            return new CustomTransaction();
        }

        @Override
        public DataStoreTransaction beginReadTransaction() {
            return new CustomTransaction();
        }
    }
}
