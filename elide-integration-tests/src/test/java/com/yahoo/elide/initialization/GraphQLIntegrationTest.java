/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.initialization;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONException;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;

import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

/**
 * Adds test methods for GraphQL IT tests.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GraphQLIntegrationTest extends IntegrationTest {

    protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public GraphQLIntegrationTest() {
        super();
    }

    public GraphQLIntegrationTest(Class<? extends ResourceConfig> resourceConfig, String packageName) {
        super(resourceConfig, packageName);
    }

    protected void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables), expected);
    }

    protected void runQueryWithExpectedResult(
            String graphQLQuery,
            Map<String, Object> variables,
            String expected,
            String apiVersion
    ) throws IOException {
        compareJsonObject(runQuery(graphQLQuery, variables, apiVersion), expected);
    }

    protected void runQueryWithExpectedResult(String graphQLQuery, String expected) throws IOException {
        runQueryWithExpectedResult(graphQLQuery, null, expected);
    }

    protected void runQueryWithExpectedError(String graphQLQuery, String errorMessage) throws IOException {
        runQueryWithExpectedError(graphQLQuery, null, errorMessage);
    }

    protected void runQueryWithExpectedError(
            String graphQLQuery,
            Map<String, Object> variables,
            String errorMessage
    ) throws IOException {
        runQuery(graphQLQuery, variables).body("errors[0].message", equalTo(errorMessage));
    }

    protected void compareJsonObject(ValidatableResponse response, String expected) {
        try {
            JSONAssert.assertEquals(expected, response.extract().body().asString(), true);
        } catch (JSONException e) {
            fail(e);
        }
    }

    protected ValidatableResponse runQuery(String query, Map<String, Object> variables,
                                           String apiVersion) throws IOException {
        return runQuery(toJsonQuery(query, variables), apiVersion);
    }

    protected ValidatableResponse runQuery(String query, Map<String, Object> variables) throws IOException {
        return runQuery(toJsonQuery(query, variables), NO_VERSION);
    }

    protected ValidatableResponse runQuery(String query, String apiVersion) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .header("ApiVersion", apiVersion)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    protected String toJsonQuery(String query, Map<String, Object> variables) throws IOException {
        return mapper.writeValueAsString(toJsonNode(query, variables));
    }

    protected JsonNode toJsonNode(String query) {
        return toJsonNode(query, new HashMap<>());
    }

    protected JsonNode toJsonNode(String query, Map<String, Object> variables) {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return graphqlNode;
    }

    public String loadGraphQLResponse(String fileName) throws IOException {
        try (InputStream in = this.getClass().getResourceAsStream("/graphql/responses/" + fileName)) {
            return new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        }
    }
}
