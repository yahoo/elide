/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.Collections;

/**
 * Test cases for RedisCache.
 */
public class RedisCacheTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final int EXPIRATION_MINUTES = 2;
    private JedisPooled jedisPool;
    private RedisServer redisServer;
    RedisCache cache;

    @BeforeEach
    public void setup() throws IOException {
        redisServer = new RedisServer(PORT);
        redisServer.start();
        jedisPool = new JedisPooled(HOST, PORT);
        cache = new RedisCache(jedisPool, EXPIRATION_MINUTES);
    }

    @AfterEach
    public void destroy() throws IOException {
        redisServer.stop();
    }

    @Test
    public void testGetNonExistent() {
        String key = "example_NonExist;{highScore;{}}{}{};;;;";
        assertEquals(null, cache.get(key));
    }

    @Test
    public void testPutResults() {
        String key = "example_PlayerStats;{highScore;{}}{}{};;;;";
        Iterable<Object> data = Collections.singletonList("xyzzy");
        QueryResult queryResult = QueryResult.builder().data(data).build();

        cache.put(key, queryResult);

        //retrive results and verify they match original.
        assertEquals(queryResult, cache.get(key));
        assertEquals("xyzzy", cache.get(key).getData().iterator().next());
    }

    // Redis server does not exist.
    @Test
    public void testPutResultsFail() throws IOException {
        destroy();
        String key = "example_PlayerStats;{highScore;{}}{}{};;;;";
        Iterable<Object> data = Collections.singletonList("xyzzy");
        QueryResult queryResult = QueryResult.builder().data(data).build();
        assertThrows(JedisConnectionException.class, () ->
                 cache.put(key, queryResult)
        );
    }

    @Test
    public void testGetResultsMulti() {
        String key = "example_PlayerStats;{highScore;{}}{}{};;;;";
        Iterable<Object> data = Collections.singletonList("xyzzy");
        QueryResult queryResult = QueryResult.builder().data(data).build();

        cache.put(key, queryResult);

        //retrive results and verify they match original.
        assertEquals(queryResult, cache.get(key));

        //retrive results again and verify they match original.
        assertEquals(queryResult, cache.get(key));

        String key1 = "example_PlayerStats1;{highScore;{}}{}{};;;;";
        Iterable<Object> data1 = Collections.singletonList("xyzz");
        QueryResult queryResult1 = QueryResult.builder().data(data).build();

        cache.put(key1, queryResult1);

        //retrive results and verify they match original.
        assertEquals(queryResult1, cache.get(key1));
    }
}
