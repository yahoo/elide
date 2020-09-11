/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.contrib.dynamicconfighelpers.compile.ConnectionDetails;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.core.NoopQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;

import org.hibernate.Session;

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@AllArgsConstructor
public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private EntityManagerFactory entityManagerFactory;
    private DataSource defaultDataSource;
    private String defaultDialect;
    private Map<String, ConnectionDetails> connectionDetailsMap;

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, DataSource defaultDataSource) {
        this(entityManagerFactory, defaultDataSource, SQLDialectFactory.getDefaultDialect().getClass().getName());
    }

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, DataSource defaultDataSource,
                    String defaultDialect) {
        this(entityManagerFactory, defaultDataSource, defaultDialect, Collections.emptyMap());
    }

    @Override
    public DataStore getDataStore() {

        MetaDataStore metaDataStore = new MetaDataStore();
        Consumer<EntityManager> txCancel = (em) -> { em.unwrap(Session.class).cancelQuery(); };

        AggregationDataStore aggregationDataStore = AggregationDataStore.builder()
                .queryEngine(new SQLQueryEngine(metaDataStore, defaultDataSource, defaultDialect, connectionDetailsMap))
                .queryLogger(new NoopQueryLogger())
                .build();

        DataStore jpaStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                (em) -> { return new NonJtaTransaction(em, txCancel); }
        );

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
    }

    public void cleanseTestData() {

    }
}
