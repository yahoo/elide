/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Relation.TO_ONE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import com.google.common.collect.ImmutableSet;

import example.Embedded;
import example.Left;
import example.Right;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.HashMap;

/**
 * Integration test for embedded collections.
 */
public class EmbeddedIT extends IntegrationTest {

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(new EntityDictionary(new HashMap<>()));
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
        Resource resource = resource(
                type("embedded"),
                id("1"),
                attributes(
                        attr("segmentIds", new int[]{3, 4, 5})
                )
        );

        given().when().get("/embedded/1").then().statusCode(HttpStatus.SC_OK).body(equalTo(datum(resource).toJSON()));
    }

    @Test
    void testOne2One() throws Exception {
        Resource resource = resource(
                type("right"),
                id("1"),
                relationships(
                        relation("noUpdate"),
                        relation("many2one", TO_ONE),
                        relation("noUpdateOne2One", TO_ONE),
                        relation("one2one", TO_ONE,
                                linkage(type("left"), id("1"))
                        ),
                        relation("noDelete")
                )
        );

        String actual = given()
                .when()
                .get("/right/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .log().all()
                .extract()
                .body().asString();

        System.out.println(datum(resource).toJSON());

        JSONAssert.assertEquals(datum(resource).toJSON(), actual, true);
    }
}
