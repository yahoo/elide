/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.errorObjectsTests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;

import com.yahoo.elide.initialization.ErrorObjectsIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.resources.JsonApiEndpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.testng.Assert;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

public class ErrorObjectsIT extends IntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    public ErrorObjectsIT() {
        super(ErrorObjectsIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    @Test
    public void testJsonAPIErrorObjects() throws IOException {
        JsonNode errors = objectMapper.readTree(
            RestAssured
                .given()
                    .contentType(JSONAPI_CONTENT_TYPE)
                    .accept(JSONAPI_CONTENT_TYPE)
                    .body(
                        datum(
                            resource(
                                    type("nocreate"),
                                    id("1")
                            )
                        )
                    )
                    .post("/nocreate")
                    .then()
                    .statusCode(HttpStatus.SC_FORBIDDEN)
                    .extract().body().asString());

        for (JsonNode errorNode : errors.get("errors")) {
            Assert.assertTrue(errorNode.isObject(), "expected error should be object");
            Assert.assertTrue(errorNode.has("detail"), "JsonAPI error should have 'detail'");
        }
    }

    @Test
    public void testGraphQLErrorObjects() throws IOException {
        // this is an incorrectly formatted query, which should result in a 400 error being thrown
        String request = "mutation { nocreate(op: UPSERT, data:{id:\"1\"}) { edges { node { id } } } }";

        JsonNode errors = objectMapper.readTree(
            RestAssured
               .given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().body().asString());

        for (JsonNode errorNode : errors.get("errors")) {
            Assert.assertTrue(errorNode.isObject(), "expected error should be object");
            Assert.assertTrue(errorNode.has("message"), "GraphQL error should have 'message'");
        }
    }
}
