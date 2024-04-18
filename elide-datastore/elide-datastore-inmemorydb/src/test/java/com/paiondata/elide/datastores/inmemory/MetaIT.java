/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.inmemory;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
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
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
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
