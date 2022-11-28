/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;

import jakarta.persistence.EntityManagerFactory;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

public class NoCacheAggregationDataStoreTestHarness extends AggregationDataStoreTestHarness {

    public NoCacheAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, ConnectionDetails defaultConnectionDetails,
            Map<String, ConnectionDetails> connectionDetailsMap, DynamicConfigValidator validator) {
        super(entityManagerFactory, defaultConnectionDetails, connectionDetailsMap, validator);
    }

    public NoCacheAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, DataSource defaultDataSource) {
        this(entityManagerFactory, new ConnectionDetails(defaultDataSource, SQLDialectFactory.getDefaultDialect()));
    }

    public NoCacheAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory,
                    ConnectionDetails defaultConnectionDetails) {
        super(entityManagerFactory, defaultConnectionDetails, Collections.emptyMap(), null);
    }

    @Override
    public DataStore getDataStore() {

        MetaDataStore metaDataStore = createMetaDataStore();

        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = createAggregationDataStoreBuilder(metaDataStore);

        DataStore jpaStore = createJPADataStore();

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStoreBuilder.build());
    }
}
