/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Resource;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test rehydration of Map of Enums.
 */
class MapEnumIT extends IntegrationTest {
    @Test
    public void testPostColorShape() {

        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("Red", "Circle");

        // Create MapColorShape using Post
        Resource resource = resource(
                type("mapColorShape"),
                id("1"),
                attributes(
                        attr("colorShapeMap", colorMap)
                )
        );
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(datum(resource))
                .post("/mapColorShape")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(resource).toJSON()));

        colorMap.clear();
        colorMap.put("Blue", "Square");

        // Update MapColorShape using Patch
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(datum(resource))
                .patch("/mapColorShape/1")
                .then().statusCode(HttpStatus.SC_NO_CONTENT);

        given()
                .accept("application/vnd.api+json")
                .get("/mapColorShape/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(resource).toJSON()));
    }

    @Test
    public void testPatchExtensionColorShape() {
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("Blue", "Triangle");

        // Create MapColorShape using Post
        Resource resource = resource(
                type("mapColorShape"),
                id("1"),
                attributes(
                        attr("colorShapeMap", colorMap)
                )
        );
        // Create MapColorShape using Patch extension
        given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body("[\n"
                        + "{\n"
                        + "  \"op\": \"add\",\n"
                        + "  \"path\": \"/mapColorShape\",\n"
                        + "  \"value\": {\n"
                        + "  \"id\": \"12345681-1234-1234-1234-1234567890ab\",\n"
                        + "  \"type\": \"mapColorShape\",\n"
                        + "  \"attributes\": {\n"
                        + "    \"colorShapeMap\": {\n"
                        + "       \"Blue\": \"Triangle\"\n"
                        + "     }\n"
                        + "   }\n"
                        + " }\n"
                        + "}\n"
                        + "]")
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .accept("application/vnd.api+json")
                .get("/mapColorShape/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(resource).toJSON()));

    }
}
