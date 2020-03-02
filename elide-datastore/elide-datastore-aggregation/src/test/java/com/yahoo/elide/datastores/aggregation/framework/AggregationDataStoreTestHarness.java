/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.METADATA_STORE_ANNOTATIONS;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.utils.ClassScanner;

import javax.persistence.EntityManagerFactory;

public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private EntityManagerFactory entityManagerFactory;

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public DataStore getDataStore() {
        MetaDataStore metaDataStore = new MetaDataStore(ClassScanner.getAnnotatedClasses(METADATA_STORE_ANNOTATIONS));

        QueryEngine sqlQueryEngine = new SQLQueryEngine(metaDataStore, entityManagerFactory);

        AggregationDataStore aggregationDataStore = new AggregationDataStore(sqlQueryEngine);

        DataStore jpaStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                NonJtaTransaction::new
        );

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
    }

    public void cleanseTestData() {

    }
}
