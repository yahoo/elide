/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.inmemory;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class MetaIT extends IntegrationTest {
    @Override
    protected DataStoreTestHarness createHarness() {
        return new WithMetaInMemoryDataStoreHarness();
    }

    @Test
    public void testEmptyFetch() {
        given()
                .when()
                .get("/widget")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("meta.foobar", equalTo(123));
    }

    @Test
    public void testCreateAndFetch() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(
                        resource(
                                type("widget"),
                                id("1")
                        )
                ).toJSON())
                .when()
                .post("/widget")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.meta.foo", equalTo("bar"));

        given()
                .when()
                .get("/widget?page[totals]")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data[0].meta.foo", equalTo("bar"))
                .body("meta.foobar", equalTo(123))
                .body("meta.page.totalRecords", equalTo(1));
    }
}
