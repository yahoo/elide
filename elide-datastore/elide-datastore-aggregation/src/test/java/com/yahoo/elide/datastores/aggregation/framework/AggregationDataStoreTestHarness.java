/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.modelconfig.compile.ConnectionDetails;
import com.yahoo.elide.modelconfig.compile.ElideDynamicEntityCompiler;
import org.hibernate.Session;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@AllArgsConstructor
public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private EntityManagerFactory entityManagerFactory;
    private ConnectionDetails defaultConnectionDetails;
    private Map<String, ConnectionDetails> connectionDetailsMap;
    private ElideDynamicEntityCompiler compiler;

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, DataSource defaultDataSource) {
        this(entityManagerFactory, new ConnectionDetails(defaultDataSource,
                        SQLDialectFactory.getDefaultDialect().getClass().getName()));
    }

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory,
                    ConnectionDetails defaultConnectionDetails) {
        this(entityManagerFactory, defaultConnectionDetails, Collections.emptyMap(), null);
    }

    @Override
    public DataStore getDataStore() {

        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = AggregationDataStore.builder();

        MetaDataStore metaDataStore;
        if (compiler != null) {
            try {
                metaDataStore = new MetaDataStore(compiler, true);
                Set<Class<?>> annotatedClasses = new HashSet<>();
                annotatedClasses.addAll(compiler.findAnnotatedClasses(FromTable.class));
                annotatedClasses.addAll(compiler.findAnnotatedClasses(FromSubquery.class));
                aggregationDataStoreBuilder.dynamicCompiledClasses(annotatedClasses);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        } else {
            metaDataStore = new MetaDataStore(true);
        }

        AggregationDataStore aggregationDataStore = aggregationDataStoreBuilder
                .queryEngine(new SQLQueryEngine(metaDataStore, defaultConnectionDetails, connectionDetailsMap))
                .queryLogger(new Slf4jQueryLogger())
                .build();

        Consumer<EntityManager> txCancel = (em) -> { em.unwrap(Session.class).cancelQuery(); };

        DataStore jpaStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                (em) -> { return new NonJtaTransaction(em, txCancel); }
        );

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
    }

    public void cleanseTestData() {

    }
}
