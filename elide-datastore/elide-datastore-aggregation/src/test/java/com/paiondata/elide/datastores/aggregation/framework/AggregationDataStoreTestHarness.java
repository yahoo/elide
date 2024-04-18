/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.framework;

import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.datastores.aggregation.AggregationDataStore;
import com.paiondata.elide.datastores.aggregation.DefaultQueryValidator;
import com.paiondata.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.query.AggregateBeforeJoinOptimizer;
import com.paiondata.elide.datastores.jpa.JpaDataStore;
import com.paiondata.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.paiondata.elide.modelconfig.validator.DynamicConfigValidator;
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
                em -> new NonJtaTransaction(em, txCancel),
                entityManagerFactory::getMetamodel
        );
    }

    protected MetaDataStore createMetaDataStore() {
        ClassScanner scanner = new DefaultClassScanner();

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
