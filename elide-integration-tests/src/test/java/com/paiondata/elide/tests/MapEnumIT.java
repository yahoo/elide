/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.initialization.IntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.elements.Resource;
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
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(datum(resource))
                .post("/mapColorShape")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(resource).toJSON()));

        colorMap.clear();
        colorMap.put("Blue", "Square");

        // Update MapColorShape using Patch
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(datum(resource))
                .patch("/mapColorShape/1")
                .then().statusCode(HttpStatus.SC_NO_CONTENT);

        given()
                .accept(JsonApi.MEDIA_TYPE)
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
                .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
                .accept(JsonApi.JsonPatch.MEDIA_TYPE)
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
                .accept(JsonApi.MEDIA_TYPE)
                .get("/mapColorShape/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(resource).toJSON()));

    }
}
