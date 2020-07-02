/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.TestInstance;

import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MediaType;

/**
 * Adds test methods for GraphQL IT tests.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GraphQLIntegrationTest extends IntegrationTest {
    protected void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables), expected);
    }

    protected void runQueryWithExpectedResult(String graphQLQuery, String expected) throws IOException {
        runQueryWithExpectedResult(graphQLQuery, null, expected);
    }

    protected void compareJsonObject(ValidatableResponse response, String expected) throws IOException {
        JsonNode responseNode = mapper.readTree(response.extract().body().asString());
        JsonNode expectedNode = mapper.readTree(expected);
        assertEquals(expectedNode, responseNode);
    }

    protected ValidatableResponse runQuery(String query, Map<String, Object> variables) throws IOException {
        return runQuery(toJsonQuery(query, variables));
    }

    protected ValidatableResponse runQuery(String query) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    protected String toJsonQuery(String query, Map<String, Object> variables) throws IOException {
        return mapper.writeValueAsString(toJsonNode(query, variables));
    }

    protected JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }
}
