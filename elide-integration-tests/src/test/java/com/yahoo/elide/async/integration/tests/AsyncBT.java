/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;

import com.yahoo.elide.async.integration.tests.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AsyncBT extends IntegrationTest {

    public AsyncBT() {
        super(AsyncIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    private static final Resource ENDERS_GAME = resource(
            type("book"),
            attributes(
                    attr("title", "Ender's Game"),
                    attr("genre", "Science Fiction"),
                    attr("language", "English")
            )
    );

    /**
     * This test demonstrates an example post request using JSON-API for a JSONAPI query.
     */
    @Test
    @Order(1)
    public void jsonApiRequestPostTest() {

      // Create Book: Ender's Game
      given()
              .contentType(JSONAPI_CONTENT_TYPE)
              .accept(JSONAPI_CONTENT_TYPE)
              .body(
                      datum(ENDERS_GAME).toJSON()
              )
              .post("/book")
              .then()
              .statusCode(HttpStatus.SC_CREATED);

        given()
        .contentType(JSONAPI_CONTENT_TYPE)
        .body(
                data(
                    resource(
                       type("asyncQuery"),
                       id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                       attributes(
                               attr("query", "/book?sort=title&fields%5Bbook%5D=genre,language"),
                               attr("queryType", "JSONAPI_V1_0"),
                               attr("status", "QUEUED")
                       )
                    )
                ).toJSON())
        .when()
        .post("/asyncQuery")
        .then()
        .statusCode(org.apache.http.HttpStatus.SC_CREATED);
     }
}
