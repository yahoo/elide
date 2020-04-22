/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.framework;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;

import javax.persistence.EntityManagerFactory;

public class AsyncDataStoreTestHarness implements DataStoreTestHarness {

    private EntityManagerFactory entityManagerFactory;

    public AsyncDataStoreTestHarness(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public DataStore getDataStore() {
        MetaDataStore metaDataStore = new MetaDataStore();

        QueryEngine sqlQueryEngine = new SQLQueryEngine(metaDataStore, entityManagerFactory);

        AggregationDataStore aggregationDataStore = new AggregationDataStore(sqlQueryEngine);

        DataStore jpaStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                NonJtaTransaction::new
        );

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
     }

    @Override
    public void cleanseTestData() {
    }
}
