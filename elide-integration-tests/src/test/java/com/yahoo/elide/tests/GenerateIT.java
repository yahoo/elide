/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.jayway.restassured.RestAssured.given;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static org.hamcrest.Matchers.notNullValue;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import org.junit.jupiter.api.Test;

/**
 * Tests for UserType
 */
class GenerateIT extends IntegrationTest {

    @Test
    public void testGeneratePost() {

        Resource resource = resource(
                type("generate"),
                id("1")
        );

        given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(datum(resource))
            .post("/generate")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .body("data.attributes.created", notNullValue());
    }
}
