/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.datastores.multiplex.bridgeable.BridgeableDataStoreHarness;
import com.yahoo.elide.example.beans.HibernateUser;
import com.yahoo.elide.example.hbase.beans.RedisActions;
import com.yahoo.elide.initialization.IntegrationTest;

import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;
import redis.embedded.util.OS;

import java.net.ServerSocket;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BridgeableStoreTest extends IntegrationTest {
    public static RedisServer REDIS_SERVER;
    public static Jedis REDIS_CLIENT;
    public static final String REDIS_SERVER_PROPERTY = "multiplex.redis.server.path";

    @BeforeAll
    public void setup() throws Exception {
        // Redis data
        int redisPort = getUnusedPort();
        RedisServerBuilder redisServerBuilder = new RedisServerBuilder()
                .port(redisPort);

        // Allow overriding the redis executable path with custom path on system
        String redisServerPath = System.getProperty(REDIS_SERVER_PROPERTY);
        if (redisServerPath != null) {
            redisServerBuilder = redisServerBuilder
                    .redisExecProvider(RedisExecProvider.defaultProvider()
                            .override(OS.UNIX, redisServerPath)
                            .override(OS.WINDOWS, redisServerPath)
                            .override(OS.MAC_OS_X, redisServerPath));
        }

        REDIS_SERVER = redisServerBuilder.build();
        REDIS_SERVER.start();

        REDIS_CLIENT = new Jedis("localhost", redisPort);
        REDIS_CLIENT.hmset(RedisActions.class.getCanonicalName(), ImmutableMap.<String, String>builder()
                .put("user1:1", "user1actionid1")
                .put("user1:2", "user1actionid2")
                .put("user2:3", "user2actionid1")
                .put("user2:4", "user2actionid2")
                .put("user2:5", "user2actionid3")
                .build());
    }

    @BeforeEach
    public void initTestData() throws Exception {
        // Hibernate data
        DataStoreTransaction tx = BridgeableDataStoreHarness.LATEST_HIBERNATE_STORE.beginTransaction();

        HibernateUser hUser1 = new HibernateUser();
        HibernateUser hUser2 = new HibernateUser();
        hUser1.setId(1L);
        hUser2.setId(2L);
        hUser1.setFirstName("Test");
        hUser2.setFirstName("Test2");
        hUser1.setSpecialActionId(1);
        hUser2.setSpecialActionId(5);

        tx.save(hUser1, null);
        tx.save(hUser2, null);

        tx.commit(null);
        tx.close();
    }

    @AfterAll
    public void cleanup() throws Exception {
        REDIS_CLIENT.close();
        REDIS_SERVER.stop();
    }

    @Test
    public void testFetchBridgeableStoreToMany() {
        String result = given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/1?include=redisActions")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertTrue(result.contains("user1actionid1"));
        assertTrue(result.contains("user1actionid2"));
        assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectToMany() {
        String result = given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/1/redisActions/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/2/redisActions/3")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertTrue(result.contains("user1actionid1"));
        assertFalse(result.contains("user1actionid2"));
        assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectFromBadSourceToMany() {
        String result = given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/1/redisActions/3")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .extract().body().asString();
    }

    @Test
    public void testFetchBridgeableStoreToOne() {
        String result = given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/1?include=specialAction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertTrue(result.contains("user1actionid1"));
        assertFalse(result.contains("user1actionid2"));
        assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectToOne() {
        String result = given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/1/specialAction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/2/specialAction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertTrue(result.contains("user1actionid1"));
        assertFalse(result.contains("user1actionid2"));
        assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectFromBadSourceToOne() {
        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/hibernateUser/1/redisActions/3")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .extract().body().asString();
    }

    private static int getUnusedPort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
