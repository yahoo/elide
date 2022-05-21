/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.targetEntity;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
public class TargetEntityIT extends IntegrationTest {

    @Test
    public void testEmployeeHierarchy() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("swe"),
                                        id(null),
                                        attributes(
                                                attr("name", "peon")
                                        )
                                )
                        )
                )
                .post("/swe")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("boss"),
                                        id(null),
                                        attributes(
                                                attr("name", "boss")
                                        ),
                                        relationships(
                                                relation("reports", linkage(type("swe"), id("1")))
                                        )
                                )
                        )
                )
                .post("/boss")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/boss/1")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.reports.data.id", contains("1"),
                        "data.relationships.reports.data.type", contains("swe")
                );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/swe/1")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.boss.data.id", equalTo("1"),
                        "data.relationships.boss.data.type", equalTo("boss")
                );
    }
}
