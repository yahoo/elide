/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.initialization;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.google.common.collect.Sets;
import example.Address;
import example.Company;
import example.Parent;
import example.models.generics.Manager;
import example.models.targetEntity.SWE;
import example.models.triggers.Invoice;
import example.models.versioned.BookV2;

import java.util.Set;

/**
 * Test Harness to initialize the default data store for IT tests.
 */
public class InMemoryDataStoreHarness implements DataStoreTestHarness {
    private InMemoryDataStore memoryStore;
    private HashMapDataStore mapStore;

    public InMemoryDataStoreHarness() {
        Set<Package> beanPackages = Sets.newHashSet(
                Parent.class.getPackage(),
                SWE.class.getPackage(),
                Invoice.class.getPackage(),
                Manager.class.getPackage(),
                BookV2.class.getPackage(),
                AsyncQuery.class.getPackage(),
                Company.class.getPackage(),
                Address.class.getPackage()

        );

        mapStore = new HashMapDataStore(DefaultClassScanner.getInstance(), beanPackages);
        memoryStore = new InMemoryDataStore(mapStore);
    }

    @Override
    public DataStore getDataStore() {
        return memoryStore;
    }

    @Override
    public void cleanseTestData() {
        mapStore.cleanseTestData();
    }
}
