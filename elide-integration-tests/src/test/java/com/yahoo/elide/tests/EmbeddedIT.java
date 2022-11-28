/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.test.jsonapi.elements.Relation.TO_ONE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import com.google.common.collect.ImmutableSet;
import example.Embedded;
import example.Left;
import example.Right;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Integration test for embedded collections.
 */
public class EmbeddedIT extends IntegrationTest {

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());
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

        given()
                .when()
                .get("/right/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(datum(resource), true));
    }
}
