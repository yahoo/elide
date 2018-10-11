/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import static com.jayway.restassured.RestAssured.given;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.datastores.multiplex.bridgeable.BridgeableStoreSupplier;
import com.yahoo.elide.example.beans.HibernateUser;
import com.yahoo.elide.example.hbase.beans.RedisActions;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;

import com.google.common.collect.ImmutableMap;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.Jedis;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;
import redis.embedded.util.OS;

import java.net.ServerSocket;

public class BridgeableStoreIT extends AbstractIntegrationTestInitializer {
    public static RedisServer REDIS_SERVER;
    public static Jedis REDIS_CLIENT;
    public static final String REDIS_SERVER_PROPERTY = "multiplex.redis.server.path";

    @BeforeClass
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

        // Hibernate data
        DataStoreTransaction tx = BridgeableStoreSupplier.LATEST_HIBERNATE_STORE.beginTransaction();

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

    @AfterClass
    public void cleanup() throws Exception {
        REDIS_CLIENT.close();
        REDIS_SERVER.stop();
    }

    @Test
    public void testFetchBridgeableStoreToMany() {
        String result = given()
                .accept("application/vnd.api+json")
                .get("/hibernateUser/1?include=redisActions")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        Assert.assertTrue(result.contains("user1actionid1"));
        Assert.assertTrue(result.contains("user1actionid2"));
        Assert.assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectToMany() {
        String result = given()
                .accept("application/vnd.api+json")
                .get("/hibernateUser/1/redisActions/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        given()
                .accept("application/vnd.api+json")
                .get("/hibernateUser/2/redisActions/3")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        Assert.assertTrue(result.contains("user1actionid1"));
        Assert.assertFalse(result.contains("user1actionid2"));
        Assert.assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectFromBadSourceToMany() {
        String result = given()
                .accept("application/vnd.api+json")
                .get("/hibernateUser/1/redisActions/3")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .extract().body().asString();
    }

    @Test
    public void testFetchBridgeableStoreToOne() {
        String result = given()
                .accept("application/vnd.api+json")
                .get("/hibernateUser/1?include=specialAction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        Assert.assertTrue(result.contains("user1actionid1"));
        Assert.assertFalse(result.contains("user1actionid2"));
        Assert.assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectToOne() {
        String result = given()
                .accept("application/vnd.api+json")
                .get("/hibernateUser/1/specialAction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        given()
                .accept("application/vnd.api+json")
                .get("/hibernateUser/2/specialAction")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        Assert.assertTrue(result.contains("user1actionid1"));
        Assert.assertFalse(result.contains("user1actionid2"));
        Assert.assertFalse(result.contains("user2"));
    }

    @Test
    public void testFetchBridgeableStoreLoadSingleObjectFromBadSourceToOne() {
        String result = given()
                .accept("application/vnd.api+json")
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
