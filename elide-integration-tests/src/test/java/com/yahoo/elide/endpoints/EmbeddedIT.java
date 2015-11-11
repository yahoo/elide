/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.hibernate.AHibernateTest;
import com.yahoo.elide.jsonapi.JsonApiMapper;

import com.google.common.collect.ImmutableSet;
import example.Embedded;
import example.Left;
import example.Right;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * The type Config resource test.
 */
// TODO: These tests (i.e. the whole suite) are too tightly coupled. We need to refactor them.
public class EmbeddedIT extends AHibernateTest {
    private final JsonApiMapper mapper;

    public EmbeddedIT() {
        /* There is no good way to get the dictionary from Elide */
        EntityDictionary empty = new EntityDictionary();

        /* Empty dictionary is OK provided the OBJECT_MAPPER is used for reading only */
        mapper = new JsonApiMapper(empty);
    }

    @BeforeTest
    public static void setup() throws IOException {
        DataStoreTransaction tx = hibernateManager.beginTransaction();
        Embedded embedded = new Embedded(); // id 1
        embedded.setSegmentIds(ImmutableSet.of(3L, 4L, 5L));

        tx.save(embedded);

        Left left = new Left();
        Right right = new Right();

        left.setOne2one(right);
        right.setOne2one(left);

        tx.save(left);
        tx.save(right);

        tx.commit();
    }

    @Test
    void testEmbedded() {
        String expected = getJson("/EmbeddedIT/testEmbedded.json");

        given().when().get("/embedded/1").then().statusCode(HttpStatus.SC_OK).body(equalTo(expected));
    }

    @Test
    void testOne2One() {
        String expected = getJson("/EmbeddedIT/testOne2One.json");

        String response =
                given().when().get("/right/1")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(response, expected);
    }

    @Test
    void testOne2OneAccess() {
        String expected = getJson("/EmbeddedIT/testOne2OneAccess.json");

        String response =
                given().when().get("/right/1/one2one/one2one")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        assertEqualDocuments(response, expected);
    }
}
