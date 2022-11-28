/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.errorObjectsTests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.initialization.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

public class ErrorObjectsIT extends IntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testJsonAPIErrorObjects() throws IOException {
        JsonNode errors = objectMapper.readTree(
            given()
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
            assertTrue(errorNode.isObject(), "expected error should be object");
            assertTrue(errorNode.has("detail"), "JsonAPI error should have 'detail'");
        }
    }

    @Test
    public void testGraphQLErrorObjects() throws IOException {
        // this is an incorrectly formatted query, which should result in a 400 error being thrown
        String request = "mutation { nocreate(op: UPSERT, data:{id:\"1\"}) { edges { node { id } } } }";

        JsonNode errors = objectMapper.readTree(
            given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().body().asString());

        for (JsonNode errorNode : errors.get("errors")) {
            assertTrue(errorNode.isObject(), "expected error should be object");
            assertTrue(errorNode.has("message"), "GraphQL error should have 'message'");
        }
    }
}
