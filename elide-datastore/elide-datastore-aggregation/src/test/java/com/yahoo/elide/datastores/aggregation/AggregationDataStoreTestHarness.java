/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;

public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private QueryEngineFactory queryEngineFactory;

    public AggregationDataStoreTestHarness(QueryEngineFactory queryEngineFactory) {
        this.queryEngineFactory = queryEngineFactory;
    }

    @Override
    public DataStore getDataStore() {
        MetaDataStore metaDataStore = new MetaDataStore(PlayerStats.class.getPackage());
        AggregationDataStore aggregationDataStore = new AggregationDataStore(queryEngineFactory, metaDataStore);


        // meta data store needs to be put at first to populate meta data models
        return new MultiplexManager(metaDataStore, aggregationDataStore);
    }

    public void cleanseTestData() {

    }
}
