/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.integration;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.framework.RedisAggregationDataStoreTestHarness;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Map;
import javax.persistence.EntityManagerFactory;

/**
 * Integration tests for {@link AggregationDataStore} using Redis for cache.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RedisAggregationDataStoreIntegrationTest extends AggregationDataStoreIntegrationTest {
    private static final int PORT = 6379;

    private RedisServer redisServer;

    public RedisAggregationDataStoreIntegrationTest() {
        super();
    }

    @BeforeAll
    @Override
    public void beforeAll() {
        super.beforeAll();
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

        ConnectionDetails defaultConnectionDetails = createDefaultConnectionDetails();

        EntityManagerFactory emf = createEntityManagerFactory();

        Map<String, ConnectionDetails> connectionDetailsMap = createConnectionDetailsMap(defaultConnectionDetails);

        return new RedisAggregationDataStoreTestHarness(emf, defaultConnectionDetails, connectionDetailsMap, VALIDATOR);
    }
}
