/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.initialization;

import static com.yahoo.elide.core.EntityDictionary.NO_VERSION;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.response.ValidatableResponse;

import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MediaType;

public class GraphQLTestUtils {

    private ObjectMapper mapper = new ObjectMapper();

    public void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables), expected);
    }

    public void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected,
            String apiVersion
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables, apiVersion), expected);
    }

    public void runQueryWithExpectedResult(String graphQLQuery, String expected) throws IOException {
        runQueryWithExpectedResult(graphQLQuery, null, expected);
    }

    public void compareJsonObject(ValidatableResponse response, String expected) throws IOException {
        JsonNode responseNode = mapper.readTree(response.extract().body().asString());
        JsonNode expectedNode = mapper.readTree(expected);
        assertEquals(expectedNode, responseNode);
    }

    public ValidatableResponse runQuery(String query, Map<String, Object> variables) throws IOException {
        return runQuery(toJsonQuery(query, variables), NO_VERSION);
    }

    public ValidatableResponse runQuery(String query, Map<String, Object> variables, String apiVersion)
            throws IOException {
        return runQuery(toJsonQuery(query, variables), apiVersion);
    }

    public ValidatableResponse runQuery(String query, String apiVersion) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("ApiVersion", apiVersion)
                .body(query)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    public String toJsonArray(JsonNode... nodes) throws IOException {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        for (JsonNode node : nodes) {
            arrayNode.add(node);
        }
        return mapper.writeValueAsString(arrayNode);
    }

    public String toJsonQuery(String query, Map<String, Object> variables) throws IOException {
        return mapper.writeValueAsString(toJsonNode(query, variables));
    }

    public JsonNode toJsonNode(String query) {
        return toJsonNode(query, null);
    }

    public JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }
}
