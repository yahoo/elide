/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngineFactory;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;

public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private SQLQueryEngineFactory queryEngineFactory;

    public AggregationDataStoreTestHarness(SQLQueryEngineFactory queryEngineFactory) {
        this.queryEngineFactory = queryEngineFactory;
    }

    @Override
    public DataStore getDataStore() {
        MetaDataStore metaDataStore = new MetaDataStore();

        AggregationDataStore aggregationDataStore = new AggregationDataStore(queryEngineFactory, metaDataStore);

        DataStore jpaStore = new JpaDataStore(
                () -> queryEngineFactory.getEmf().createEntityManager(),
                NonJtaTransaction::new
        );

        // meta data store needs to be put at first to populate meta data models
        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
    }

    public void cleanseTestData() {

    }
}
