/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.storageengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.async.models.TableExport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Test cases for RedisResultStorageEngine.
 */
public class RedisResultStorageEngineTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final int EXPIRATION_SECONDS = 120;
    private static final int BATCH_SIZE = 2;
    private static final boolean EXTENSION_SUPPORT = false;
    private JedisPooled jedisPool;
    private RedisServer redisServer;
    RedisResultStorageEngine engine;

    @BeforeEach
    public void setup() throws IOException {
        redisServer = new RedisServer(PORT);
        redisServer.start();
        jedisPool = new JedisPooled(HOST, PORT);
        engine = new RedisResultStorageEngine(jedisPool, EXTENSION_SUPPORT, EXPIRATION_SECONDS, BATCH_SIZE);
    }

    @AfterEach
    public void destroy() throws IOException {
        redisServer.stop();
    }

    @Test
    public void testReadNonExistent() {
        assertThrows(IllegalStateException.class, () ->
                verifyResults("nonexisting_results", Arrays.asList(""))
        );
    }

    @Test
    public void testStoreEmptyResults() {
        String queryId = "store_empty_results_success";
        String validOutput = "";
        String[] input = validOutput.split("\n");

        storeResults(queryId, Observable.fromArray(input));

        // verify contents of stored files are readable and match original
        verifyResults("store_empty_results_success", Arrays.asList(validOutput));
    }

    @Test
    public void testStoreResults() {
        String queryId = "store_results_success";
        String validOutput = "hi\nhello";
        String[] input = validOutput.split("\n");

        storeResults(queryId, Observable.fromArray(input));

        // verify contents of stored files are readable and match original
        verifyResults("store_results_success", Arrays.asList(validOutput));
    }

    // Redis server does not exist.
    @Test
    public void testStoreResultsFail() throws IOException {
        destroy();
        assertThrows(JedisConnectionException.class, () ->
                storeResults("store_results_fail",
                        Observable.fromArray(new String[]{"hi", "hello"}))
        );
    }

    @Test
    public void testReadResultsBatch() {
        String queryId = "store_results_batch_success";
        // 3 records > batchSize i.e 2
        String validOutput = "hi\nhello\nbye";
        String[] input = validOutput.split("\n");

        storeResults(queryId, Observable.fromArray(input));

        // 2 onnext calls will be returned.
        // 1st call will have 2 records combined together as one. hi and hello.
        // 2nd call will have 1 record only. bye.
        verifyResults("store_results_batch_success", Arrays.asList("hi\nhello", "bye"));
    }

    private void verifyResults(String queryId, List<String> expected) {
        TestObserver<String> subscriber = new TestObserver<>();

        Observable<String> observable = engine.getResultsByID(queryId);

        observable.subscribe(subscriber);
        subscriber.assertComplete();
        assertEquals(subscriber.getEvents().iterator().next(), expected);
    }

    private void storeResults(String queryId, Observable<String> storable) {
        TableExport query = new TableExport();
        query.setId(queryId);

        engine.storeResults(query, storable);
    }
}
