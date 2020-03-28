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
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import javax.persistence.Persistence;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import com.yahoo.elide.async.integration.framework.AsyncDataStoreTestHarness;
import com.yahoo.elide.async.integration.framework.AsyncIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AsyncIntegrationTest extends IntegrationTest{

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
    void jsonApiRequestPostTest() {
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
        //Adding slight delay before starting to wait for AsyncQuery to be completed
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
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\\\"]) { edges { node { id createdOn updatedOn queryType status result { edges { node { id createdOn updatedOn responseBody status} } } } } } }\",\"variables\":null}")
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.asyncQuery.edges[0].node.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                .body("data.asyncQuery.edges[0].node.createdOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.updatedOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.asyncQuery.edges[0].node.status", equalTo("COMPLETE"))
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.createdOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.updatedOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.responseBody", equalTo("{\"data\":"
                        + "[{\"type\":\"player\",\"id\":\"3\",\"attributes\":{\"name\":\"Han\"}},"
                        + "{\"type\":\"player\",\"id\":\"2\",\"attributes\":{\"name\":\"Jane Doe\"}},"
                        + "{\"type\":\"player\",\"id\":\"1\",\"attributes\":{\"name\":\"Jon Doe\"}}]}"))
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.status", equalTo(200));
    }

    /**
     * This test demonstrates an example post request using JSON-API for AsyncQuery object for a GRAPHQL query.
     */
    @Test
    @Order(5)
    void graphQlApiRequestPostTest() {
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
        //Adding slight delay before starting to wait for AsyncQuery to be completed
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
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263c\\\"]) { edges { node { id createdOn updatedOn queryType status result { edges { node { id createdOn updatedOn responseBody status} } } } } } }\",\"variables\":null}")
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.asyncQuery.edges[0].node.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"))
                .body("data.asyncQuery.edges[0].node.createdOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.updatedOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.queryType", equalTo("GRAPHQL_V1_0"))
                .body("data.asyncQuery.edges[0].node.status", equalTo("COMPLETE"))
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263c"))
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.createdOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.updatedOn", notNullValue())
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.responseBody", equalTo("{\"data\":{\"player\":{\"edges\":"
                        + "[{\"node\":{\"id\":\"1\",\"name\":\"Jon Doe\"}},"
                        + "{\"node\":{\"id\":\"2\",\"name\":\"Jane Doe\"}},"
                        + "{\"node\":{\"id\":\"3\",\"name\":\"Han\"}}]}}}"))
                .body("data.asyncQuery.edges[0].node.result.edges[0].node.status", equalTo(200));
    }
}
