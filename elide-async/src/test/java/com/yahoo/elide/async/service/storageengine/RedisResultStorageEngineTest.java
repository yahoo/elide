/*
 * Copyright 2020, Yahoo Inc.
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
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Test cases for RedisResultStorageEngine.
 */
public class RedisResultStorageEngineTest {
    private static final int PORT = 6379;
    private JedisPool jedisPool;
    private RedisServer redisServer;
    
    @BeforeEach
    public void setup() throws IOException {
    	redisServer = new RedisServer(PORT);
    	redisServer.start();
    	jedisPool = new JedisPool("localhost", PORT);
    }

    @AfterEach
    public void destroy() throws IOException {
    	redisServer.stop();
    }

    @Test
    public void testReadNonExistent() {
        assertThrows(IllegalStateException.class, () ->
                readResults("nonexisting_results")
        );
    }

    @Test
    public void testStoreEmptyResults() {
        String queryId = "store_empty_results_success";
        String validOutput = "";
        String[] input = validOutput.split("\n");

        storeResults(queryId, Observable.fromArray(input));

        // verify contents of stored files are readable and match original
        String finalResult = readResults(queryId);
        assertEquals(validOutput, finalResult);
    }

    @Test
    public void testStoreResults() {
        String queryId = "store_results_success";
        String validOutput = "hi\nhello";
        String[] input = validOutput.split("\n");

        storeResults(queryId, Observable.fromArray(input));

        // verify contents of stored files are readable and match original
        String finalResult = readResults(queryId);
        assertEquals(validOutput, finalResult);
    }

    // Redis server does not exist.
    @Test
    public void testStoreResultsFail() throws IOException {
    	destroy();
        assertThrows(IllegalStateException.class, () ->
                storeResults("store_results_fail",
                        Observable.fromArray(new String[]{"hi", "hello"}))
        );
    }

    private String readResults(String queryId) {
    	RedisResultStorageEngine engine = new RedisResultStorageEngine(jedisPool, false, 120);

        return engine.getResultsByID(queryId).collect(() -> new StringBuilder(),
                (resultBuilder, tempResult) -> {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append(System.lineSeparator());
                    }
                    resultBuilder.append(tempResult);
                }
            ).map(StringBuilder::toString).blockingGet();
    }

    private void storeResults(String queryId, Observable<String> storable) {
        RedisResultStorageEngine engine = new RedisResultStorageEngine(jedisPool, false, 120);
        TableExport query = new TableExport();
        query.setId(queryId);

        engine.storeResults(query, storable);
    }
}
