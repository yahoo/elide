/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.initialization;

import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.datastore.inmemory.InMemoryDataStore;
import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.datastores.multiplex.MultiplexManager;
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
    private HashMapDataStore asyncStore;

    public InMemoryDataStoreHarness() {
        Set<Package> beanPackages = Sets.newHashSet(
                Parent.class.getPackage(),
                SWE.class.getPackage(),
                Invoice.class.getPackage(),
                Manager.class.getPackage(),
                BookV2.class.getPackage(),
                Company.class.getPackage(),
                Address.class.getPackage()
        );

        Set<Package> asyncBeanPackages = Sets.newHashSet(
                AsyncQuery.class.getPackage()
        );


        mapStore = new HashMapDataStore(new DefaultClassScanner(), beanPackages);
        asyncStore = new HashMapDataStore(new DefaultClassScanner(), asyncBeanPackages);
        memoryStore = new InMemoryDataStore(new MultiplexManager(mapStore, asyncStore));

    }

    @Override
    public DataStore getDataStore() {
        return memoryStore;
    }

    @Override
    public void cleanseTestData() {
        mapStore.cleanseTestData();
        asyncStore.cleanseTestData();
    }
}
