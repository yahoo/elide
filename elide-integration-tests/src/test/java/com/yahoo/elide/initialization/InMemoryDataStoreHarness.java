/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.initialization;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.models.triggers.Invoice;

import com.google.common.collect.Sets;
import example.Parent;

import java.util.Set;

/**
 * Test Harness to initialize the default data store for IT tests.
 */
public class InMemoryDataStoreHarness implements DataStoreTestHarness {
    HashMapDataStore store;

    public InMemoryDataStoreHarness() {
        Set<Package> beanPackages = Sets.newHashSet(
                Parent.class.getPackage(),
                Invoice.class.getPackage(),
                Manager.class.getPackage()
        );

        store = new HashMapDataStore(beanPackages);
    }

    @Override
    public DataStore getDataStore() {
        return store;
    }

    @Override
    public void cleanseTestData() {
        store.cleanseTestData();
    }
}
