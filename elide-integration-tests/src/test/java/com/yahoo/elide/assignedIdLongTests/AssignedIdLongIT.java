/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.assignedIdLongTests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Data;
import com.yahoo.elide.initialization.IntegrationTest;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class AssignedIdLongIT extends IntegrationTest {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    @Test
    public void testResponseCodeOnUpdate() {
        Data original = datum(
                resource(
                        type("assignedIdLong"),
                        id("1"),
                        attributes(
                                attr("value", 3)
                        )
                )
        );

        Data modified = datum(
                resource(
                        type("assignedIdLong"),
                        id("1"),
                        attributes(
                                attr("value", 9)
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(original)
                .post("/assignedIdLong")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(original.toJSON()));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(modified)
                .patch("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
