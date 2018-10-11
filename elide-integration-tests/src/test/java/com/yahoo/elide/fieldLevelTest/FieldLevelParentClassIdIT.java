/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.fieldLevelTest;

import static com.jayway.restassured.RestAssured.given;

import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

public class FieldLevelParentClassIdIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    @Test(priority = 0)
    public void testResponseCodeOnUpdate() {
        String request = jsonParser.getJson("/FieldLevelIT/createFieldLevelChildEntity.req.json");
        String expected = jsonParser.getJson("/FieldLevelIT/createFieldLevelChildEntity.resp.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/fieldLevelChild")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();
        assertEqualDocuments(actual, expected);

        request = jsonParser.getJson("/FieldLevelIT/updateFieldLevelChildEntity.req.json");

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(request)
            .patch("/fieldLevelChild/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
