/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.integration;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.framework.RedisAggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

/**
 * Integration tests for {@link AggregationDataStore}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisAggregationDataStoreIntegrationTest extends AggregationDataStoreIntegrationTest {
    private static final int PORT = 6379;

    private RedisServer redisServer;

    static {
        VALIDATOR = new DynamicConfigValidator(DefaultClassScanner.getInstance(), "src/test/resources/configs");

        try {
            VALIDATOR.readAndValidateConfigs();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public RedisAggregationDataStoreIntegrationTest() {
        super();
    }

    @BeforeAll
    @Override
    public void beforeAll() {
        SQLUnitTest.init();
        try {
            redisServer = new RedisServer(PORT);
            redisServer.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    public void afterEverything() {
        try {
            redisServer.stop();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected DataStoreTestHarness createHarness() {

        HikariConfig config = new HikariConfig(File.separator + "jpah2db.properties");
        DataSource defaultDataSource = new HikariDataSource(config);
        SQLDialect defaultDialect = SQLDialectFactory.getDefaultDialect();
        ConnectionDetails defaultConnectionDetails = new ConnectionDetails(defaultDataSource, defaultDialect);

        Properties prop = new Properties();
        prop.put("javax.persistence.jdbc.driver", config.getDriverClassName());
        prop.put("javax.persistence.jdbc.url", config.getJdbcUrl());
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("aggregationStore", prop);

        Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

        // Add an entry for "mycon" connection which is not from hjson
        connectionDetailsMap.put("mycon", defaultConnectionDetails);
        // Add connection details fetched from hjson
        VALIDATOR.getElideSQLDBConfig().getDbconfigs().forEach(dbConfig ->
            connectionDetailsMap.put(dbConfig.getName(),
                            new ConnectionDetails(getDataSource(dbConfig, getDBPasswordExtractor()),
                                            SQLDialectFactory.getDialect(dbConfig.getDialect())))
        );

        return new RedisAggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
    }
}
