/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;

import com.google.common.collect.ImmutableSet;

import example.Embedded;
import example.Left;
import example.Right;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Integration test for embedded collections.
 */
public class EmbeddedIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public static void setup() throws IOException {
        DataStoreTransaction tx = dataStore.beginTransaction();
        Embedded embedded = new Embedded(); // id 1
        embedded.setSegmentIds(ImmutableSet.of(3L, 4L, 5L));

        tx.createObject(embedded, null);

        Left left = new Left();
        Right right = new Right();

        left.setOne2one(right);
        right.setOne2one(left);

        tx.createObject(left, null);
        tx.createObject(right, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    void testEmbedded() {
        String expected = jsonParser.getJson("/EmbeddedIT/testEmbedded.json");

        given().when().get("/embedded/1").then().statusCode(HttpStatus.SC_OK).body(equalTo(expected));
    }

    @Test
    void testOne2One() {
        String expected = jsonParser.getJson("/EmbeddedIT/testOne2One.json");

        String actual =
                given().when().get("/right/1")
                        .then().statusCode(HttpStatus.SC_OK)
                        .extract().body().asString();

        assertEqualDocuments(actual, expected);
    }
}
