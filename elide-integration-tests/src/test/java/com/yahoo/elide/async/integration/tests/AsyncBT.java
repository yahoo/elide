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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    public void jsonApiRequestPostTest() throws InterruptedException {

       //Create Book: Ender's Game
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
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(ENDERS_GAME).toJSON()
                )
                .get("/book")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                        attributes(
                                                attr("query", "/book"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery");

        int i = 0;
        Response response = null;
        while (i < 1000) {
            response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d");
            if (response.getStatusCode() == 200) {
                break;
            }
            Thread.sleep(10);
        }

        System.out.println("Response obj from Async test");
        System.out.println(response.asString());

        Thread.sleep(1000);
     }
}
