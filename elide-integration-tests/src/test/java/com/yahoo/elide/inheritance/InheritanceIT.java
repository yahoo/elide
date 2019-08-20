/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.inheritance;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InheritanceIT extends IntegrationTest {

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    @Test
    public void testEmployeeHierarchy() {

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("manager"),
                                        id(null)
                                )
                        )
                )
                .post("/manager")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("employee"),
                                        id(null),
                                        attributes(),
                                        relationships(
                                                relation("boss",
                                                        linkage(type("manager"), id("1"))
                                                )
                                        )
                                )
                        )
                )
                .post("/manager/1/minions")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"),
                        "data.relationships.boss.data.id", equalTo("1")
                );

        given()
                .contentType("application/vnd.api+json")
                .when()
                .get("/manager/1")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.minions.data.id", contains("1"),
                        "data.relationships.minions.data.type", contains("employee")
                );
    }
}
