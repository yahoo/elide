/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests
import static com.jayway.restassured.RestAssured.given
import static org.testng.Assert.assertFalse
import static org.testng.Assert.assertTrue

import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer

import org.testng.annotations.Test
/**
 * Tests for UserType
 */
class GenerateIT extends AbstractIntegrationTestInitializer {

    @Test(priority = 1)
    public void testGeneratePost() {

        String body = """
        {
            "data": {
                "id": 1,
                "type": "generate",
                "attributes": {
                }
            }
        }
        """

        String resp = given()
            .contentType("application/vnd.api+json")
            .accept("application/vnd.api+json")
            .body(body)
            .post("/generate")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().body().asString()

        assertTrue(resp.contains("created"), "Generated field is missing\n" + resp)
        assertFalse(resp.contains(":null"), "Generated field is null\n" + resp)
    }
}
