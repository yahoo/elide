/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.AggregateBeforeJoinOptimizer;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import org.hibernate.Session;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

@AllArgsConstructor
@Getter
public abstract class AggregationDataStoreTestHarness implements DataStoreTestHarness {
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

    protected JpaDataStore createJPADataStore() {
        Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();

        return new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                em -> new NonJtaTransaction(em, txCancel)
        );
    }

    protected MetaDataStore createMetaDataStore() {
        ClassScanner scanner = DefaultClassScanner.getInstance();

        MetaDataStore metaDataStore;
        if (validator != null) {
           metaDataStore = new MetaDataStore(scanner,
                   validator.getElideTableConfig().getTables(),
                   validator.getElideNamespaceConfig().getNamespaceconfigs(), true);
        } else {
            metaDataStore = new MetaDataStore(scanner, true);
        }

        return metaDataStore;
    }

    protected AggregationDataStore.AggregationDataStoreBuilder createAggregationDataStoreBuilder(MetaDataStore metaDataStore) {
        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = AggregationDataStore.builder();

        if (validator != null) {
            aggregationDataStoreBuilder.dynamicCompiledClasses(metaDataStore.getDynamicTypes());
        }
        return aggregationDataStoreBuilder
                .queryEngine(new SQLQueryEngine(metaDataStore,
                        (name) -> connectionDetailsMap.getOrDefault(name, defaultConnectionDetails),
                        new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore))),
                        new DefaultQueryPlanMerger(metaDataStore),
                        new DefaultQueryValidator(metaDataStore.getMetadataDictionary())))
                .queryLogger(new Slf4jQueryLogger());
    }

    @Override
    public void cleanseTestData() {

    }
}
