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


import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.integration.tests.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.async.models.QueryType;
import com.yahoo.elide.async.models.ResultType;
import com.yahoo.elide.audit.TestAuditLogger;
import com.yahoo.elide.contrib.testhelpers.graphql.EnumFieldSerializer;
import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;
import com.yahoo.elide.resources.SecurityContextUser;
import com.yahoo.elide.security.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.Data;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncIT extends IntegrationTest {

    @Data
    private class AsyncQuery {

        private String id;
        private String query;

        @JsonSerialize(using = EnumFieldSerializer.class, as = String.class)
        private String queryType;
        private Integer asyncAfterSeconds;

        @JsonSerialize(using = EnumFieldSerializer.class, as = String.class)
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

    public AsyncIT() {
        super(AsyncIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        DataStoreTestHarness dataStoreTestHarness = super.createHarness();
        return new DataStoreTestHarness() {
                public DataStore getDataStore() {
                    return new AsyncDelayDataStore(dataStoreTestHarness.getDataStore(), 5000);
                }
                public void cleanseTestData() {
                    dataStoreTestHarness.cleanseTestData();
                }
        };
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

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/asyncQuery/edc4a871-dff2-4054-804e-d80075cf830e");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Async Query is created and completed
            if (outputResponse.equals("COMPLETE")) {

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
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Async Query has failed.");
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
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
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
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { id title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"PROCESSING\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            String responseGraphQL = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{\"query\":\"{ asyncQuery(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf828e\\\"]) "
                            + "{ edges { node { id queryType status result "
                            + "{ responseBody httpStatus resultType contentLength } } } } }\","
                            + "\"variables\":null}")
                    .post("/graphQL")
                    .asString();
            // If Async Query is created and completed
            if (responseGraphQL.contains("\"status\":\"COMPLETE\"")) {

                expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                        + "\"result\":{\"responseBody\":\"{\\\"data\\\":{\\\"book\\\":{\\\"edges\\\":[{\\\"node\\\":{\\\"id\\\":\\\"1\\\",\\\"title\\\":\\\"Ender's Game\\\"}},"
                        + "{\\\"node\\\":{\\\"id\\\":\\\"2\\\",\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                        + "{\\\"node\\\":{\\\"id\\\":\\\"3\\\",\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}}]}}}\","
                        + "\"httpStatus\":200,\"resultType\":\"EMBEDDED\",\"contentLength\":177}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            } else if (!(responseGraphQL.contains("\"status\":\"PROCESSING\""))) {
                fail("Async Query has failed.");
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
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("QUEUED");
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
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf829e\","
                + "\"query\":\"{\\\"query\\\":\\\"{ book { edges { node { id title } } } }\\\",\\\"variables\\\":null}\","
                + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\"}}]}}}";
        assertEquals(expectedResponse, response.extract().body().asString());

        String responseGraphQL = given()
                 .contentType(MediaType.APPLICATION_JSON)
                 .accept(MediaType.APPLICATION_JSON)
                 .body("{\"query\":\"{ asyncQuery(ids: [\\\"edc4a871-dff2-4054-804e-d80075cf829e\\\"]) "
                         + "{ edges { node { id queryType status result "
                         + "{ responseBody httpStatus resultType contentLength } } } } }\","
                         + "\"variables\":null}")
                 .post("/graphQL")
                 .asString();

        expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"edc4a871-dff2-4054-804e-d80075cf829e\",\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\","
                 + "\"result\":{\"responseBody\":\"{\\\"data\\\":{\\\"book\\\":{\\\"edges\\\":[{\\\"node\\\":{\\\"id\\\":\\\"1\\\",\\\"title\\\":\\\"Ender's Game\\\"}},"
                 + "{\\\"node\\\":{\\\"id\\\":\\\"2\\\",\\\"title\\\":\\\"Song of Ice and Fire\\\"}},"
                 + "{\\\"node\\\":{\\\"id\\\":\\\"3\\\",\\\"title\\\":\\\"For Whom the Bell Tolls\\\"}}]}}}\","
                 + "\"httpStatus\":200,\"resultType\":\"EMBEDDED\",\"contentLength\":177}}}]}}}";

        assertEquals(expectedResponse, responseGraphQL);
    }

    /**
     * Test for QueryStatus Set to PROCESSING instead of Queued
     */
    @Test
    public void graphQLTestCreateFailOnQueryStatus() {

        AsyncDelayStoreTransaction.sleep = true;
        AsyncQuery queryObj = new AsyncQuery();
        queryObj.setId("edc4a871-dff2-4054-804e-d80075cf839e");
        queryObj.setAsyncAfterSeconds(0);
        queryObj.setQueryType("GRAPHQL_V1_0");
        queryObj.setStatus("PROCESSING");
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
        ValidatableResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLJsonNode)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK);

        assertEquals(true, response.extract().body().asString().contains("errors"));
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

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {
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
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Async Query has failed.");
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

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Async Query is created and completed
            if (outputResponse.equals("COMPLETE")) {
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
            } else if (!(outputResponse.equals("PROCESSING"))) {
                fail("Async Query has failed.");
                break;
            }
            i++;

            if (i == 1000) {
                fail("Async Query not completed.");
            }
        }
    }

    /**
     * Tests Read Permissions on Async Model for Admin Role
     * @throws InterruptedException
     */
    @Test
    public void asyncModelAdminReadPermissions() throws IOException {

        ElideResponse response = null;
        String id = "edc4a871-dff2-4054-804e-d80075c08959";
        String query = "test-query";

        com.yahoo.elide.async.models.AsyncQuery queryObj = new com.yahoo.elide.async.models.AsyncQuery();
        queryObj.setId(id);
        queryObj.setQuery(query);
        queryObj.setQueryType(QueryType.JSONAPI_V1_0);
        queryObj.setPrincipalName("owner-user");

        dataStore.populateEntityDictionary(
                        new EntityDictionary(AsyncIntegrationTestApplicationResourceConfig.MAPPINGS));
        DataStoreTransaction tx = dataStore.beginTransaction();
        tx.createObject(queryObj, null);
        tx.commit(null);
        tx.close();

        Elide elide = new Elide(new ElideSettingsBuilder(dataStore)
                        .withEntityDictionary(
                                        new EntityDictionary(AsyncIntegrationTestApplicationResourceConfig.MAPPINGS))
                        .withAuditLogger(new TestAuditLogger()).build());

        User ownerUser = new User(() -> "owner-user");
        SecurityContextUser securityContextAdminUser = new SecurityContextUser(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> "1";
            }
            @Override
            public boolean isUserInRole(String s) {
                return true;
            }
            @Override
            public boolean isSecure() {
                return false;
            }
            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });
        SecurityContextUser securityContextNonAdminUser = new SecurityContextUser(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> "2";
            }
            @Override
            public boolean isUserInRole(String s) {
                return false;
            }
            @Override
            public boolean isSecure() {
                return false;
            }
            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });

        // Principal is Owner
        response = elide.get("/asyncQuery/" + id, new MultivaluedHashMap<>(), ownerUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        // Principal has Admin Role
        response = elide.get("/asyncQuery/" + id, new MultivaluedHashMap<>(), securityContextAdminUser, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());

        // Principal without Admin Role
        response = elide.get("/asyncQuery/" + id, new MultivaluedHashMap<>(), securityContextNonAdminUser, NO_VERSION);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getResponseCode());
    }

    /**
     * Reset sleep delay flag after each test
     */
    @AfterEach
    public void sleepDelayReset() {

        AsyncDelayStoreTransaction.sleep = false;
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
