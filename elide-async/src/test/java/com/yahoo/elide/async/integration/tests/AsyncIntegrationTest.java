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
     * This test demonstrates an example post request using the JSON-API for Async.
     */
    @Test
    @Order(1)
    void jsonApiPostTest() {
        given()
        .contentType(JSONAPI_CONTENT_TYPE)
        .body(
                data(
                    resource(
                       type("query"),
                       id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                       attributes(
                               attr("query", "/player?sort=name&fields%5Bgroup%5D=name"),
                               attr("queryType", "JSONAPI_V1_0"),
                               attr("status", "QUEUED")
                       )
                    )
                ).toJSON())
        .when()
        .post("/query")
        .then()
        .statusCode(org.apache.http.HttpStatus.SC_CREATED);
     }

    /**
     * This test demonstrates an example get request using the JSON-API for Async.
     * @throws InterruptedException
     */
    @Test
    @Order(2)
    public void jsonApiAsyncQueryGetTest() throws InterruptedException {
        //Adding slight delay before starting to wait for AsyncQuery to be completed
        Thread.sleep(15000);
        given()
                .accept("application/vnd.api+json")
                .get("/query/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                .body("data.attributes.createdOn", notNullValue())
                .body("data.attributes.updatedOn", notNullValue())
                .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                .body("data.attributes.status", equalTo("COMPLETE"))
                .body("data.relationships.result.data.type", equalTo("queryResult"))
                .body("data.relationships.result.data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"));
    }

}
