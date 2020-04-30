/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.*;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.async.integration.tests.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;

import javax.ws.rs.core.MediaType;

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

    private static final Resource GAME_OF_THRONES = resource(
            type("book"),
            attributes(
                    attr("title", "Song of Ice and Fire"),
                    attr("genre", "Mythology Fiction"),
                    attr("language", "English")
            )
    );

    private static final Resource FOR_WHOM_THE_BELL_TOLLS = resource(
            type("book"),
            attributes(
                    attr("title", "For Whom the Bell Tolls"),
                    attr("genre", "Literary Fiction"),
                    attr("language", "English")
            )
    );

    /**
     * Creates test data for all tests.
     */
    @BeforeAll
    public static void init() {
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
                        datum(GAME_OF_THRONES).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(FOR_WHOM_THE_BELL_TOLLS).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

    }

    /**
     * This test demonstrates an example post request using JSON-API for a JSONAPI query.
     */
    @Test
    public void jsonApiRequestTests() throws InterruptedException {

        Response responseCreated = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery");

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d");

            // If Async Query is created and completed
            if (response.jsonPath().getString("data.attributes.status").equals("COMPLETE")) {

                // Check AsyncQuery GET
                response.then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                .body("data.type", equalTo("asyncQuery"))
//                .body("data.attributes.createdOn", notNullValue())
//                .body("data.attributes.updatedOn", notNullValue())
                .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.relationships.result.data.type", equalTo("asyncQueryResult"))
                .body("data.relationships.result.data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"));

                // Validate AsyncQueryResult Response
                given()
                        .accept("application/vnd.api+json")
                        .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d/result")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                        .body("data.type", equalTo("asyncQueryResult"))
//                        .body("data.attributes.createdOn", notNullValue())
//                        .body("data.attributes.updatedOn", notNullValue())
                        .body("data.attributes.contentLength", notNullValue())
                        .body("data.attributes.responseBody", equalTo("{\"data\":"
                                + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                                + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                                + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}"))
                        .body("data.attributes.status", equalTo(200))
                        .body("data.relationships.query.data.type", equalTo("asyncQuery"))
                        .body("data.relationships.query.data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ edges { node { id responseBody status} } } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphQL")
                        .asString();

                String expectedResponse = document(
                        selections(
                                field(
                                        "asyncQuery",
                                        selections(
                                                field("id", "ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                                field("queryType", "JSONAPI_V1_0"),
                                                field("status", "COMPLETE"),
                                                field("result",
                                                        selections(
                                                                field("id", "ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                                                field("responseBody", "{\\\"data\\\":"
                                                                        + "[{\\\"type\\\":\\\"book\\\",\\\"id\\\":\\\"3\\\",\\\"attributes\\\":{\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}}"
                                                                        + ",{\\\"type\\\":\\\"book\\\",\\\"id\\\":\\\"2\\\",\\\"attributes\\\":{\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                                                                        + "{\\\"type\\\":\\\"book\\\",\\\"id\\\":\\\"1\\\",\\\"attributes\\\":{\\\"title\\\":\\\"Ender's Game\\\"}}]}"),
                                                                field("status", 200)
                                                        ))
                                        )
                                )
                        )
                ).toResponse();

                assertEquals(expectedResponse, responseGraphQL);

                break;
            }
        }
     }
}
