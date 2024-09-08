/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.IdObfuscationTestApplicationResourceConfig;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.test.jsonapi.elements.Resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests for IdObfuscator.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IdObfuscationIT extends IntegrationTest {

    public IdObfuscationIT() {
        super(IdObfuscationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Test
    void testIdObfuscation() {
        // Create
        String dataId = given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("customerInvoice"),
                                        attributes(
                                                attr("complete", true),
                                                attr("total", 1000)
                                        )
                                )
                        )
                )
                .post("/customerInvoice")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.attributes.total", equalTo(1100))
                .body("data.attributes.complete", equalTo(true))
                .extract()
                .path("data.id");
        assertTrue(dataId.length() > 10);
        // Get
        Resource resource = resource(
                type("customerInvoice"),
                id(dataId),
                attributes(
                        attr("complete", true),
                        attr("total", 1100)
                )
        );
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .get("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(resource).toJSON()));

        // Patch
        Resource modified = resource(
                type("customerInvoice"),
                id(dataId),
                attributes(
                        attr("complete", true),
                        attr("total", 123456)
                )
        );

        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(datum(modified))
                .patch("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Get again
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .get("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(modified).toJSON()));

        // Get list
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .get("/customerInvoice")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(data(modified).toJSON()));

        // Delete
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .delete("/customerInvoice/" + dataId)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
