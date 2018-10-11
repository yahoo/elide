/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.errorObjectsTests;

import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.initialization.ErrorObjectsIntegrationTestApplicationResourceConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

public class ErrorObjectsIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    public ErrorObjectsIT() {
        super(ErrorObjectsIntegrationTestApplicationResourceConfig.class);
    }

    @Test
    public void testJsonAPIErrorObjects() throws IOException {
        String request = "{ \"data\": { \"type\": \"nocreate\", \"id\": \"1\" } }";

        JsonNode errors = objectMapper.readTree(
                RestAssured
                        .given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .body(request)
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
        String request = "mutation { nocreate(op: UPSERT, data:{id:\"1\"}) { edges { node { id } } } }";

        JsonNode errors = objectMapper.readTree(
                RestAssured
                        .given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(request)
                        .post("/graphQL")
                        .then()
                        .statusCode(HttpStatus.SC_FORBIDDEN)
                        .extract().body().asString());

        for (JsonNode errorNode : errors.get("errors")) {
            Assert.assertTrue(errorNode.isObject(), "expected error should be object");
            Assert.assertTrue(errorNode.has("message"), "GraphQL error should have 'message'");
        }
    }
}
