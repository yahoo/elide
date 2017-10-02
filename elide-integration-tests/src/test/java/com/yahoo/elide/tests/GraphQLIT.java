/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;

import java.io.IOException;


/**
 * Simple integration tests to verify session and access.
 */
public class GraphQLIT extends AbstractIntegrationTestInitializer {
    private static final String ATTRIBUTES = "attributes";
    private static final String RELATIONSHIPS = "relationships";
    private static final String INCLUDED = "included";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public void setup() throws Exception {
        String graphQLRequestData = "mutation {\n" +
                "  book(op:UPSERT, data:[{id: \"123\", title: \"book1\", authors: [{id: \"789\", name: \"author1\"}]}, {id: \"456\", \"title\": \"book2\", \"authors\": [{id: \"012\", name: \"author2\"}]}]) {\n" +
                "    edges {\n" +
                "      node {\n" +
                "        id\n" +
                "        authors {\n" +
                "          edges {\n" +
                "            node {\n" +
                "              id\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("query", graphQLRequestData);
        String graphQLRequest = objectMapper.writeValueAsString(node);

        String x = RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLRequest)
                .post("/graphQL")
                .then()
                .extract().asString();
//                .statusCode(HttpStatus.SC_OK);

        Assert.assertTrue(false);
    }


    @Test
    public void fetchRootSingle() throws IOException {
        String graphQLQuery = "{ book(ids: [\\\"1\\\"]) { edges { node { id title } } } }";
        String graphQLRequest = "{ \"query\": \"" + graphQLQuery + "\" }";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLRequest)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
