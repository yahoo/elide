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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;

import javax.persistence.EntityManagerFactory;

public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private EntityManagerFactory entityManagerFactory;

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public DataStore getDataStore() {
        MetaDataStore metaDataStore = new MetaDataStore();

        AggregationDataStore aggregationDataStore = AggregationDataStore.builder()
                .queryEngine(new SQLQueryEngine(metaDataStore, entityManagerFactory))
                .build();

        DataStore jpaStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                NonJtaTransaction::new
        );

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
    }

    public void cleanseTestData() {

    }
}
