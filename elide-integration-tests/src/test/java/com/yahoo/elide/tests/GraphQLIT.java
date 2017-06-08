/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;
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

    @Test
    public void fetchRootSingle() throws IOException {
        String graphQLRequest = "{ book(id: \"1\") { id title } }";

        RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(graphQLRequest)
                .post("/graphQL")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
    }
}
