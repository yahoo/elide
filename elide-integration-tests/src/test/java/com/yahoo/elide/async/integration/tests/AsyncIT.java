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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.async.integration.tests.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;

import javax.ws.rs.core.MediaType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncIT extends IntegrationTest {

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

    public AsyncIT() {
        super(AsyncIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    /**
     * Creates test data for all tests.
     */
    @BeforeEach
    public void init() {
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
     * Test for a JSONAPI query as a Async Request with asyncAfterSeconds value set to 10.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiRequestAsyncAfterTests() throws InterruptedException {

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("requestId", "1001"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        // Validate AsyncQueryResult Response
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.attributes.result.contentLength", notNullValue())
                .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                        + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                        + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                        + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}"))
                .body("data.attributes.result.httpStatus", equalTo(200))
                .body("data.attributes.result.resultType", equalTo(ResultType.EMBEDDED.toString()));

    }

    /**
     * Various tests for a JSONAPI query as a Async Request with asyncAfterSeconds value set to 0.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiRequestTests() throws InterruptedException {

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("requestId", "1001"),
                                                attr("asyncAfterSeconds", "0")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d");

            // If Async Query is created and completed
            if (response.jsonPath().getString("data.attributes.status").equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"));

                // Validate AsyncQueryResult Response
                given()
                        .accept("application/vnd.api+json")
                        .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                                + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                                + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                                + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}"))
                        .body("data.attributes.result.httpStatus", equalTo(200))
                        .body("data.attributes.result.resultType", equalTo(ResultType.EMBEDDED.toString()));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus resultType contentLength } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphQL")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"responseBody\":\"{\\\"data\\\":[{\\\"type\\\":\\\"book\\\",\\\"id\\\":\\\"3\\\",\\\"attributes\\\":{\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}},"
                        + "{\\\"type\\\":\\\"book\\\",\\\"id\\\":\\\"2\\\",\\\"attributes\\\":{\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                        + "{\\\"type\\\":\\\"book\\\",\\\"id\\\":\\\"1\\\",\\\"attributes\\\":{\\\"title\\\":\\\"Ender's Game\\\"}}]}\","
                        + "\"httpStatus\":200,\"resultType\":\"EMBEDDED\",\"contentLength\":218}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }

    /**
     * Various tests for a GRAPHQL query as a Async Request.
     * @throws InterruptedException
     */
    @Test
    public void graphQLRequestTests() throws InterruptedException {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"),
                                        attributes(
                                                attr("query", "{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}"),
                                                attr("queryType", "GRAPHQL_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("requestId", "1001"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263c");

            // If Async Query is created and completed then validate results
            if (response.jsonPath().getString("data.attributes.status").equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"));

                // Validate AsyncQueryResult Response
                given()
                        .accept("application/vnd.api+json")
                        .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263c")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":{\"book\":{\"edges\":"
                                + "[{\"node\":{\"id\":\"1\",\"title\":\"Ender's Game\"}},"
                                + "{\"node\":{\"id\":\"2\",\"title\":\"Song of Ice and Fire\"}},"
                                + "{\"node\":{\"id\":\"3\",\"title\":\"For Whom the Bell Tolls\"}}]}}}"))
                        .body("data.attributes.result.httpStatus", equalTo(200))
                        .body("data.attributes.result.resultType", equalTo(ResultType.EMBEDDED.toString())).toString();

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263c\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus resultType contentLength } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphQL")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263c\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"responseBody\":\"{\\\"data\\\":{\\\"book\\\":{\\\"edges\\\":[{\\\"node\\\":{\\\"id\\\":\\\"1\\\",\\\"title\\\":\\\"Ender's Game\\\"}},"
                        + "{\\\"node\\\":{\\\"id\\\":\\\"2\\\",\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                        + "{\\\"node\\\":{\\\"id\\\":\\\"3\\\",\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}}]}}}\","
                        + "\"httpStatus\":200,\"resultType\":\"EMBEDDED\",\"contentLength\":177}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }

    /**
     * Various tests for an unknown collection (group) that does not exist JSONAPI query as a Async Request.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiUnknownRequestTests() throws InterruptedException {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"),
                                        attributes(
                                                attr("query", "/group?sort=genre&fields%5Bgroup%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("requestId", "1001"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263b");

            // If Async Query is created and completed then validate results
            if (response.jsonPath().getString("data.attributes.status").equals("COMPLETE")) {
                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"));

                // Validate AsyncQueryResult Response
                given()
                        .accept("application/vnd.api+json")
                        .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263b")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"errors\":[{\"detail\":\"Unknown collection group\"}]}"))
                        .body("data.attributes.result.httpStatus", equalTo(404));

             // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263b\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus resultType contentLength } } } } }\""
                                + ",\"variables\":null}")
                        .post("/graphQL")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263b\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"result\":{\"responseBody\":\"{\\\"errors\\\":[{\\\"detail\\\":\\\"Unknown collection group\\\"}]}\",\"httpStatus\":404,\"resultType\":\"EMBEDDED\",\"contentLength\":50}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);

                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }

    /**
     * Various tests for making a Async request for AsyncQuery request that does not exist.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiBadRequestTests() throws InterruptedException {

        //JSON API bad request
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263a")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors[0].detail", equalTo("Unknown identifier ba31ca4e-ed8f-4be0-a0f3-12088fa9263a for asyncQuery"));

      //GRAPHQL bad request
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263a\\\"]) "
                        + "{ edges { node { id createdOn updatedOn queryType status result "
                        + "{ responseBody httpStatus resultType contentLength } } } } }\""
                        + ",\"variables\":null}")
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.asyncQuery", nullValue())
                .body("errors[0].message", equalTo("Exception while fetching data (/asyncQuery) : Unknown identifier "
                        + "[ba31ca4e-ed8f-4be0-a0f3-12088fa9263a] for asyncQuery"));

    }

    /**
     * Various tests for making a Async request to a model to which the user does not have permissions.
     * @throws InterruptedException
     */
    @Test
    public void noReadEntityTests() throws InterruptedException {
        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e"),
                                        attributes(
                                                attr("query", "/noread"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("requestId", "1001"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e");

            // If Async Query is created and completed
            if (response.jsonPath().getString("data.attributes.status").equals("COMPLETE")) {
                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"));

                // Validate AsyncQueryResult Response
                given()
                        .accept("application/vnd.api+json")
                        .get("/asyncQuery/0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":[]}"))
                        .body("data.attributes.result.httpStatus", equalTo(200))
                        .body("data.attributes.result.resultType", equalTo(ResultType.EMBEDDED.toString()));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus resultType contentLength } } } } }\""
                                + ",\"variables\":null}")
                        .post("/graphQL")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"0b0dd4e7-9cdc-4bbc-8db2-5c1491c5ee1e\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"result\":{\"responseBody\":\"{\\\"data\\\":[]}\",\"httpStatus\":200,\"resultType\":\"EMBEDDED\",\"contentLength\":11}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);

                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }
}
