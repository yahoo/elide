/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import org.hibernate.Session;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@AllArgsConstructor
public class AggregationDataStoreTestHarness implements DataStoreTestHarness {
    private EntityManagerFactory entityManagerFactory;
    private ConnectionDetails defaultConnectionDetails;
    private Map<String, ConnectionDetails> connectionDetailsMap;
    private DynamicConfigValidator validator;

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, DataSource defaultDataSource) {
        this(entityManagerFactory, new ConnectionDetails(defaultDataSource, SQLDialectFactory.getDefaultDialect()));
    }

    public AggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory,
                    ConnectionDetails defaultConnectionDetails) {
        this(entityManagerFactory, defaultConnectionDetails, Collections.emptyMap(), null);
    }

    @Override
    public DataStore getDataStore() {

        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = AggregationDataStore.builder();

        ClassScanner scanner = DefaultClassScanner.getInstance();

        MetaDataStore metaDataStore;
        if (validator != null) {
           metaDataStore = new MetaDataStore(scanner,
                   validator.getElideTableConfig().getTables(),
                   validator.getElideNamespaceConfig().getNamespaceconfigs(), true);

           aggregationDataStoreBuilder.dynamicCompiledClasses(metaDataStore.getDynamicTypes());
        } else {
            metaDataStore = new MetaDataStore(scanner, true);
        }

        AggregationDataStore aggregationDataStore = aggregationDataStoreBuilder
                .queryEngine(new SQLQueryEngine(metaDataStore, defaultConnectionDetails, connectionDetailsMap,
                        new HashSet<>(), new DefaultQueryValidator(metaDataStore.getMetadataDictionary())))
                .queryLogger(new Slf4jQueryLogger())
                .build();

        Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();

        DataStore jpaStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                em -> new NonJtaTransaction(em, txCancel)
        );

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
    }

    @Override
    public void cleanseTestData() {

    }
}
