/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.async.integration.framework.AsyncDataStoreTestHarness;
import com.yahoo.elide.async.integration.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import javax.persistence.Persistence;
import javax.ws.rs.core.MediaType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AsyncIntegrationTest extends IntegrationTest {

    public AsyncIntegrationTest() {
        super(AsyncIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Override
    protected DataStoreTestHarness createHarness() {
        return new AsyncDataStoreTestHarness(Persistence.createEntityManagerFactory("asyncStore"));
    }

    /**
     * This test demonstrates an example post request using JSON-API for a JSONAPI query.
     */
    @Test
    @Order(1)
    public void jsonApiRequestPostTest() {
        given()
        .contentType(JSONAPI_CONTENT_TYPE)
        .body(
                data(
                    resource(
                       type("asyncQuery"),
                       id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                       attributes(
                               attr("query", "/player?sort=name&fields%5Bplayer%5D=id,name"),
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

    /**
     * This test demonstrates an example get request of an AsyncQuery object using JSON-API for a JSONAPI query.
     * @throws InterruptedException
     */
    @Test
    @Order(2)
    public void jsonApiRequestAsyncQueryGetTest() throws InterruptedException {
        //Adding slight delay before starting to wait for previous test to be completed
        Thread.sleep(5000);
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.createdOn", notNullValue())
                .body("data.attributes.updatedOn", notNullValue())
                .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.relationships.result.data.type", equalTo("asyncQueryResult"))
                .body("data.relationships.result.data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"));
    }

    /**
     * This test demonstrates an example get request of an AsyncQueryResult object using JSON-API for a JSONAPI query.
     */
    @Test
    @Order(3)
    public void jsonApiRequestAsyncQueryResultGetTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d/result")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                .body("data.type", equalTo("asyncQueryResult"))
                .body("data.attributes.createdOn", notNullValue())
                .body("data.attributes.updatedOn", notNullValue())
                .body("data.attributes.contentLength", notNullValue())
                .body("data.attributes.responseBody", equalTo("{\"data\":"
                        + "[{\"type\":\"player\",\"id\":\"3\",\"attributes\":{\"name\":\"Han\"}},"
                        + "{\"type\":\"player\",\"id\":\"2\",\"attributes\":{\"name\":\"Jane Doe\"}},"
                        + "{\"type\":\"player\",\"id\":\"1\",\"attributes\":{\"name\":\"Jon Doe\"}}]}"))
                .body("data.attributes.status", equalTo(200))
                .body("data.relationships.query.data.type", equalTo("asyncQuery"))
                .body("data.relationships.query.data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"));
    }

    /**
     * This test demonstrates an example get request of an AsyncQuery and its AsyncQueryResult object using GRAPHQL for a JSONAPI query.
     */
    @Test
    @Order(4)
    public void jsonApiRequestGraphQLGetTest() {

        String response = given()
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
                                                                + "[{\\\"type\\\":\\\"player\\\",\\\"id\\\":\\\"3\\\",\\\"attributes\\\":{\\\"name\\\":\\\"Han\\\"}},"
                                                                + "{\\\"type\\\":\\\"player\\\",\\\"id\\\":\\\"2\\\",\\\"attributes\\\":{\\\"name\\\":\\\"Jane Doe\\\"}},"
                                                                + "{\\\"type\\\":\\\"player\\\",\\\"id\\\":\\\"1\\\",\\\"attributes\\\":{\\\"name\\\":\\\"Jon Doe\\\"}}]}"),
                                                        field("status", 200)
                                                        ))
                                )
                        )
                )
        ).toResponse();

        assertEquals(expectedResponse, response);
    }

    /**
     * This test demonstrates an example post request using JSON-API for AsyncQuery object for a GRAPHQL query.
     */
    @Test
    @Order(5)
    public void graphQlApiRequestPostTest() {
        given()
        .contentType(JSONAPI_CONTENT_TYPE)
        .body(
                data(
                    resource(
                       type("asyncQuery"),
                       id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"),
                       attributes(
                               attr("query", "{\"query\":\"{ player { edges { node { id name } } } }\",\"variables\":null}"),
                               attr("queryType", "GRAPHQL_V1_0"),
                               attr("status", "QUEUED")
                       )
                    )
                ).toJSON())
        .when()
        .post("/asyncQuery")
        .then()
        .statusCode(org.apache.http.HttpStatus.SC_CREATED);
     }

    /**
     * This test demonstrates an example get request of an AsyncQuery object using JSON-API for a GRAPHQL query.
     * @throws InterruptedException
     */
    @Test
    @Order(6)
    public void graphQLApiRequestAsyncQueryGetTest() throws InterruptedException {
        //Adding slight delay before starting to wait for previous test to be completed
        Thread.sleep(5000);
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263c")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.createdOn", notNullValue())
                .body("data.attributes.updatedOn", notNullValue())
                .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.relationships.result.data.type", equalTo("asyncQueryResult"))
                .body("data.relationships.result.data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"));
    }

    /**
     * This test demonstrates an example get request of an AsyncQueryResult object using JSON-API for a GRAPHQL query.
     */
    @Test
    @Order(7)
    public void graphQlApiRequestAsyncQueryResultGetTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263c/result")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"))
                .body("data.type", equalTo("asyncQueryResult"))
                .body("data.attributes.createdOn", notNullValue())
                .body("data.attributes.updatedOn", notNullValue())
                .body("data.attributes.contentLength", notNullValue())
                .body("data.attributes.responseBody", equalTo("{\"data\":{\"player\":{\"edges\":"
                        + "[{\"node\":{\"id\":\"1\",\"name\":\"Jon Doe\"}},"
                        + "{\"node\":{\"id\":\"2\",\"name\":\"Jane Doe\"}},"
                        + "{\"node\":{\"id\":\"3\",\"name\":\"Han\"}}]}}}"))
                .body("data.attributes.status", equalTo(200))
                .body("data.relationships.query.data.type", equalTo("asyncQuery"))
                .body("data.relationships.query.data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"));
    }


    /**
     * This test demonstrates an example get request of an AsyncQuery and its AsyncQueryResult object using GRAPHQL for a GRAPHQL query..
     */
    @Test
    @Order(8)
    public void graphQlApiRequestGraphQLGetTest() {
        String response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263c\\\"]) "
                        + "{ edges { node { id queryType status result "
                        + "{ edges { node { id responseBody status} } } } } } }\""
                        + ",\"variables\":null}")
                .post("/graphQL")
                .asString();

        String expectedResponse = document(
                selections(
                        field(
                                "asyncQuery",
                                selections(
                                        field("id", "ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"),
                                        field("queryType", "GRAPHQL_V1_0"),
                                        field("status", "COMPLETE"),
                                        field("result",
                                                selections(
                                                        field("id", "ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"),
                                                        field("responseBody", "{\\\"data\\\":{\\\"player\\\":{\\\"edges\\\":"
                                                                + "[{\\\"node\\\":{\\\"id\\\":\\\"1\\\",\\\"name\\\":\\\"Jon Doe\\\"}}"
                                                                + ",{\\\"node\\\":{\\\"id\\\":\\\"2\\\",\\\"name\\\":\\\"Jane Doe\\\"}},"
                                                                + "{\\\"node\\\":{\\\"id\\\":\\\"3\\\",\\\"name\\\":\\\"Han\\\"}}]}}}"),
                                                        field("status", 200)
                                                        ))
                                )
                        )
                )
        ).toResponse();

        assertEquals(expectedResponse, response);

    }

    /**
     * This test demonstrates an example post request using JSON-API for a bad request JSONAPI query.
     * The test is making a Async request for a collection (group) that does not exist.
     */
    @Test
    @Order(9)
    public void jsonApiBadRequestPostTest() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .body(
                    data(
                        resource(
                           type("asyncQuery"),
                           id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"),
                           attributes(
                                   attr("query", "/group?sort=name&fields%group%5D=id,name"),
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

    /**
     * This test demonstrates an example get request of an AsyncQuery object using JSON-API for a bad request JSONAPI query.
     * The test is making a Async request for a collection (group) that does not exist.
     * @throws InterruptedException
     */
    @Test
    @Order(10)
    public void jsonApiBadRequestAsyncQueryGetTest() throws InterruptedException {
        //Adding slight delay before starting to wait for previous test to be completed
        Thread.sleep(5000);
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263b")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"))
                .body("data.type", equalTo("asyncQuery"))
                .body("data.attributes.createdOn", notNullValue())
                .body("data.attributes.updatedOn", notNullValue())
                .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.attributes.status", equalTo("FAILURE"))
                .body("data.relationships.result.data", nullValue());
    }

    /**
     * This test demonstrates an example get request of an AsyncQuery and its AsyncQueryResult object using GRAPHQL for a bad request JSONAPI query.
     * The test is making a Async request for a collection (group) that does not exist.
     */
    @Test
    @Order(11)
    public void jsonApibadRequestGraphQLGetTest() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263b\\\"]) "
                        + "{ edges { node { id queryType status result "
                        + "{ edges { node { id responseBody status} } } } } } }\""
                        + ",\"variables\":null}")
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.asyncQuery.edges[0].node.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263b"))
                .body("data.asyncQuery.edges[0].node.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.asyncQuery.edges[0].node.status", equalTo("FAILURE"));
    }

    /**
     * This test demonstrates an example get request of an AsyncQuery object using JSON-API for a JSONAPI query.
     * The test is making a Async request for AsyncQuery object that does not exist.
     * @throws InterruptedException
     */
    @Test
    @Order(12)
    public void jsonApiBadGetRequestAsyncQueryGetTest() throws InterruptedException {
        given()
                .accept("application/vnd.api+json")
                .get("/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263a")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors[0].detail", equalTo("Unknown identifier ba31ca4e-ed8f-4be0-a0f3-12088fa9263a for asyncQuery"));
    }

    /**
     * This test demonstrates an example get request of an AsyncQuery and its AsyncQueryResult object using GRAPHQL for a GRAPHQL query..
     */
    @Test
    @Order(13)
    public void graphQlApibadGetRequestGraphQLGetTest() {
         given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263a\\\"]) "
                    + "{ edges { node { id createdOn updatedOn queryType status result "
                    + "{ edges { node { id createdOn updatedOn responseBody status} } } } } } }\""
                    + ",\"variables\":null}")
            .post("/graphQL")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.asyncQuery", nullValue())
            .body("errors[0].message", equalTo("Exception while fetching data (/asyncQuery) : Unknown identifier "
                    + "[ba31ca4e-ed8f-4be0-a0f3-12088fa9263a] for asyncQuery"));
    }
}
