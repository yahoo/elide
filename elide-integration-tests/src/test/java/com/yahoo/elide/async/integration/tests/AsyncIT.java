/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.UNQUOTED_VALUE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;

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

import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.contrib.testhelpers.graphql.VariableFieldSerializer;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;
import lombok.Data;

import java.util.Map;

import javax.ws.rs.core.MediaType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncIT extends AsyncIntegrationTest {

    @Data
    private class AsyncQuery {

        private String id;
        private String query;

        @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
        private String queryType;
        private Integer asyncAfterSeconds;
        private String requestId;

        @JsonSerialize(using = VariableFieldSerializer.class, as = String.class)
        private String status;
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
     * Various tests for a JSONAPI query as a Async Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException
     */
    @Test
    public void jsonApiHappyPath1() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("edc4a871-dff2-4054-804e-d80075cf830e"),
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
                .statusCode(org.apache.http.HttpStatus.SC_CREATED)
                .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf830e"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.status", equalTo("PROCESSING"))
                .body("data.attributes.result.contentLength", nullValue())
                .body("data.attributes.result.responseBody", nullValue())
                .body("data.attributes.result.httpStatus", nullValue())
                .body("data.attributes.result.resultType", nullValue());

        AsyncDelayStoreTransaction.sleep = false;
        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/edc4a871-dff2-4054-804e-d80075cf830e");

            // If Async Query is created and completed
            if (response.jsonPath().getString("data.attributes.status").equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf830e"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                                + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                                + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                                + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}"))
                        .body("data.attributes.result.httpStatus", equalTo(200))
                        .body("data.attributes.result.resultType", equalTo(ResultType.EMBEDDED.toString()));


                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }

    /**
     * Various tests for a JSONAPI query as a Async Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException
     */
    @Test
    public void jsonApiHappyPath2() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;

        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("edc4a871-dff2-4054-804e-d80075cf831f"),
                                        attributes(
                                                attr("query", "/book?sort=genre&fields%5Bbook%5D=title"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("requestId", "1001"),
                                                attr("asyncAfterSeconds", "7")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED)
                .body("data.id", equalTo("edc4a871-dff2-4054-804e-d80075cf831f"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.attributes.result.contentLength", notNullValue())
                .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                        + "[{\"type\":\"book\",\"id\":\"3\",\"attributes\":{\"title\":\"For Whom the Bell Tolls\"}}"
                        + ",{\"type\":\"book\",\"id\":\"2\",\"attributes\":{\"title\":\"Song of Ice and Fire\"}},"
                        + "{\"type\":\"book\",\"id\":\"1\",\"attributes\":{\"title\":\"Ender's Game\"}}]}"))
                .body("data.attributes.result.httpStatus", equalTo(200))
                .body("data.attributes.result.resultType", equalTo(ResultType.EMBEDDED.toString()));

        AsyncDelayStoreTransaction.sleep = false;
    }

    /**
     * Test for a GraphQL query as a Async Request with asyncAfterSeconds value set to 0.
     * Happy Path Test Scenario 1
     * @throws InterruptedException
     */
    @Test
    public void graphQLHappyPath1() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;
        AsyncQuery queryObj = new AsyncQuery();
         queryObj.setId("edc4a871-dff2-4054-804e-d80075cf828e");
         queryObj.setAsyncAfterSeconds(0);
         queryObj.setQueryType("#GRAPHQL_V1_0");
         queryObj.setStatus("#QUEUED");
         queryObj.setQuery("{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}");
         String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "asyncQuery",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status")
                                         )
                                 )
                         )
                 )
         ).toQuery();

         JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
         given()
         .contentType(MediaType.APPLICATION_JSON)
         .accept(MediaType.APPLICATION_JSON)
         .body(graphQLJsonNode)
         .post("/graphQL")
         .then()
         .statusCode(org.apache.http.HttpStatus.SC_OK);

        AsyncDelayStoreTransaction.sleep = false;
        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            String graphQLStatus = document(
                    selection(
                            field(
                                    "asyncQuery",
                                    arguments(
                                            argument("ids", "\"edc4a871-dff2-4054-804e-d80075cf828e\"")
                                    ),
                                    selections(
                                            field("status")
                                    )
                            )
                    )
            ).toQuery();
            String response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(toJsonNode(graphQLStatus, null))
                    .post("/graphQL")
                    .asString();
            // If Async Query is created and completed
            if (response.equals("{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"status\":\"COMPLETE\"}}]}}}")) {
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf828e\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus resultType contentLength } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphQL")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
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
     * Test for a GraphQL query as a Async Request with asyncAfterSeconds value set to 7.
     * Happy Path Test Scenario 2
     * @throws InterruptedException
     */
    @Test
    public void graphQLHappyPath2() throws InterruptedException {

        AsyncDelayStoreTransaction.sleep = true;
        AsyncQuery queryObj = new AsyncQuery();
         queryObj.setId("edc4a871-dff2-4054-804e-d80075cf829e");
         queryObj.setAsyncAfterSeconds(7);
         queryObj.setQueryType("#GRAPHQL_V1_0");
         queryObj.setStatus("#QUEUED");
         queryObj.setQuery("{\"query\":\"{ book { edges { node { id title } } } }\",\"variables\":null}");
         String graphQLRequest = document(
                 mutation(
                         selection(
                                 field(
                                         "asyncQuery",
                                         arguments(
                                                 argument("op", "UPSERT"),
                                                 argument("data", queryObj, UNQUOTED_VALUE)
                                         ),
                                         selections(
                                                 field("id"),
                                                 field("query"),
                                                 field("queryType"),
                                                 field("status")
                                         )
                                 )
                         )
                 )
         ).toQuery();

         JsonNode graphQLJsonNode = toJsonNode(graphQLRequest, null);
         given()
         .contentType(MediaType.APPLICATION_JSON)
         .accept(MediaType.APPLICATION_JSON)
         .body(graphQLJsonNode)
         .post("/graphQL")
         .then()
         .statusCode(org.apache.http.HttpStatus.SC_OK);

         String responseGraphQL = given()
                 .contentType(MediaType.APPLICATION_JSON)
                 .accept(MediaType.APPLICATION_JSON)
                 .body("{\"query\":\"{ asyncQuery(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf829e\\\"]) "
                         + "{ edges { node { id queryType status result "
                         + "{ responseBody httpStatus resultType contentLength } } } } }\","
                         + "\"variables\":null}")
                 .post("/graphQL")
                 .asString();

         String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf829e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                 + "\"result\":{\"responseBody\":\"{\\\"data\\\":{\\\"book\\\":{\\\"edges\\\":[{\\\"node\\\":{\\\"id\\\":\\\"1\\\",\\\"title\\\":\\\"Ender's Game\\\"}},"
                 + "{\\\"node\\\":{\\\"id\\\":\\\"2\\\",\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                 + "{\\\"node\\\":{\\\"id\\\":\\\"3\\\",\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}}]}}}\","
                 + "\"httpStatus\":200,\"resultType\":\"EMBEDDED\",\"contentLength\":177}}}]}}}";

         assertEquals(expectedResponse, responseGraphQL);
         AsyncDelayStoreTransaction.sleep = false;
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
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"errors\":[{\"detail\":\"Unknown collection group\"}]}"))
                        .body("data.attributes.result.httpStatus", equalTo(404));

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

    }

    /**
     * Various tests for making a Async request for AsyncQuery request that does not exist.
     * @throws InterruptedException
     */
    @Test
    public void graphQLBadRequestTests() throws InterruptedException {

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
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":[]}"))
                        .body("data.attributes.result.httpStatus", equalTo(200))
                        .body("data.attributes.result.resultType", equalTo(ResultType.EMBEDDED.toString()));

                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }

    private JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }
}
