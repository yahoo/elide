/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.assignedIdLongTests;

import static com.jayway.restassured.RestAssured.given;

import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.initialization.AssignedIdLongIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.utils.JsonParser;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

public class AssignedIdLongIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    public AssignedIdLongIT() {
        super(AssignedIdLongIntegrationTestApplicationResourceConfig.class);
    }

    @Test(priority = 0)
    public void testResponseCodeOnUpdate() {
        String request = jsonParser.getJson("/AssignedIdLongIT/createAssignedIdLongEntity.req.json");
        String expected = jsonParser.getJson("/AssignedIdLongIT/createAssignedIdLongEntity.resp.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/assignedIdLong")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();
        assertEqualDocuments(actual, expected);

        request = jsonParser.getJson("/AssignedIdLongIT/updateAssignedIdLong.req.json");
        expected = jsonParser.getJson("/AssignedIdLongIT/updateAssignedIdLong.resp.json");

        actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .patch("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertEqualDocuments(actual, expected);
    }
}
