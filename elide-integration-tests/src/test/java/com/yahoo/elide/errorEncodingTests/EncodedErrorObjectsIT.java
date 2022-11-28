/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.errorEncodingTests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.utils.JsonParser;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class EncodedErrorObjectsIT extends IntegrationTest {

    private static final String GRAPHQL_CONTENT_TYPE = "application/json";
    private final JsonParser jsonParser = new JsonParser();

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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidCollectionErrorObject.json");
        given()
            .get("/unknown")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .body(equalTo(expected));
    }

    @Test
    public void invalidEntityBodyException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidEntityBodyExceptionErrorObject.json");
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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidObjectIdentifierExceptionErrorObject.json");
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
        Resource requestBody = resource(
                type("invoice"),
                id("a")
        );

        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/invalidValueExceptionErrorObject.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(requestBody))
                .post("/invoice")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void jsonPatchExtensionException() {
        String request = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionException.req.json");
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/jsonPatchExtensionExceptionErrorObject.json");
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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/transactionExceptionErrorObject.json");
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request).post("/invoice")
            .then()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
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
        String expected = jsonParser.getJson("/EncodedErrorResponsesIT/graphQLFetchErrorObjectEncoded.json");
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
