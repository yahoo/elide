/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.framework;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.cache.RedisCache;
import com.yahoo.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.AggregateBeforeJoinOptimizer;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import org.hibernate.Session;

import redis.clients.jedis.JedisPooled;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

public class RedisAggregationDataStoreTestHarness extends AggregationDataStoreTestHarness {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final int EXPIRATION_MINUTES = 2;

    public RedisAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, ConnectionDetails defaultConnectionDetails,
            Map<String, ConnectionDetails> connectionDetailsMap, DynamicConfigValidator validator) {
        super(entityManagerFactory, defaultConnectionDetails, connectionDetailsMap, validator);
    }

    public RedisAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory, DataSource defaultDataSource) {
        super(entityManagerFactory, new ConnectionDetails(defaultDataSource, SQLDialectFactory.getDefaultDialect()));
    }

    public RedisAggregationDataStoreTestHarness(EntityManagerFactory entityManagerFactory,
                    ConnectionDetails defaultConnectionDetails) {
        super(entityManagerFactory, defaultConnectionDetails, Collections.emptyMap(), null);
    }

    @Override
    public DataStore getDataStore() {
        JedisPooled jedisPool = new JedisPooled(HOST, PORT);
        RedisCache cache = new RedisCache(jedisPool, EXPIRATION_MINUTES);

        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = AggregationDataStore.builder();

        ClassScanner scanner = DefaultClassScanner.getInstance();

        MetaDataStore metaDataStore;
        if (getValidator() != null) {
           metaDataStore = new MetaDataStore(scanner,
                   getValidator().getElideTableConfig().getTables(),
                   getValidator().getElideNamespaceConfig().getNamespaceconfigs(), true);

           aggregationDataStoreBuilder.dynamicCompiledClasses(metaDataStore.getDynamicTypes());
        } else {
            metaDataStore = new MetaDataStore(scanner, true);
        }

        AggregationDataStore aggregationDataStore = aggregationDataStoreBuilder
                .queryEngine(new SQLQueryEngine(metaDataStore,
                        (name) -> getConnectionDetailsMap().getOrDefault(name, getDefaultConnectionDetails()),
                        new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore))),
                        new DefaultQueryPlanMerger(metaDataStore),
                        new DefaultQueryValidator(metaDataStore.getMetadataDictionary())))
                .queryLogger(new Slf4jQueryLogger())
                .cache(cache)
                .build();

        Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();

        DataStore jpaStore = new JpaDataStore(
                () -> getEntityManagerFactory().createEntityManager(),
                em -> new NonJtaTransaction(em, txCancel)
        );

        return new MultiplexManager(jpaStore, metaDataStore, aggregationDataStore);
    }
}
