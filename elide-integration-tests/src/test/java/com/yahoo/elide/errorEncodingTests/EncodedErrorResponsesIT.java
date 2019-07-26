/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.errorEncodingTests;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.initialization.EncodedErrorResponsesTestApplicationResourceConfig;
import com.yahoo.elide.utils.JsonParser;

import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test class for checking encoding of error response messages.
 */
public class EncodedErrorResponsesIT extends AbstractIntegrationTestInitializer {

    private static final String GRAPHQL_CONTENT_TYPE = "application/json";
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";
    private static final String JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION =
            "application/vnd.api+json; ext=jsonpatch";
    private final JsonParser jsonParser = new JsonParser();

    public EncodedErrorResponsesIT() {
        super(EncodedErrorResponsesTestApplicationResourceConfig.class);
    }

    @BeforeClass
    public void setup() throws Exception {
        tearDownServer();
        setUpServer();
    }

    @Test
    public void invalidAttributeException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/InvalidAttributeException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidAttributeException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void invalidCollectionException() {
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidCollection.json");
        given().when().get("/unknown").then().statusCode(HttpStatus.SC_NOT_FOUND).body(equalTo(expected));
    }

    @Test
    public void invalidEntityBodyException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
        .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void invalidObjectIdentifierException() {
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidObjectIdentifierExecption.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/100")
        .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo(expected));
    }


    @Test
    public void invalidValueException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/invalidValueException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidValueException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/invoice")
        .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void jsonPatchExtensionException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request).patch()
        .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void transactionException() {
        // intentionally forget the comma between type and id to force a transaction exception
        String request = "{\"data\": {\"type\": \"invoice\" \"id\": 100}}";
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/transactionException.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request).post("/invoice")
        .then()
                .statusCode(HttpStatus.SC_LOCKED)
                .body(equalTo(expected));
    }

    @Test
    public void graphQLMutationError() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLMutationError.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLMutationError.json");
        given()
                .contentType(GRAPHQL_CONTENT_TYPE)
                .accept(GRAPHQL_CONTENT_TYPE)
                .body(request)
                .post("/graphQL")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void graphQLFetchError() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLFetchError.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLFetchError.json");
        given()
                .contentType(GRAPHQL_CONTENT_TYPE)
                .accept(GRAPHQL_CONTENT_TYPE)
                .body(request)
                .post("/graphQL")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }
}
