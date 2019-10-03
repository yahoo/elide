/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Relation.TO_ONE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import example.Left;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

import java.util.HashMap;

/**
 * @Shareable annotation integration tests
 */
class ShareableIT extends IntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        dataStore.populateEntityDictionary(new EntityDictionary(new HashMap<>()));
        DataStoreTransaction tx = dataStore.beginTransaction();
        Left left = new Left();
        tx.createObject(left, null);
        tx.commit(null);
        tx.close();
    }

    @Test
    public void testUnshareableForbiddenAccess() {
        // Create container
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("container"),
                                        id(null)
                                )
                        )
                )
                .post("/container")
                .then().statusCode(HttpStatus.SC_CREATED);

        // Create unshareable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("unshareable"),
                                        id(null)
                                )
                        )
                )
                .post("/unshareable")
                .then().statusCode(HttpStatus.SC_CREATED);

        // Fail to add unshareable to container's unshareables (unshareable is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("unshareable"),
                                        id("1")
                                )
                        )
                )
                .patch("/container/1/relationships/unshareables")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to replace container's unshareables collection (unshareable is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("unshareable"),
                                        id("1")
                                )
                        )
                )
                .post("/container/1/relationships/unshareables")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to update unshareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("container"),
                                        id("1")
                                )
                        )
                )
                .patch("/unshareable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to set unshareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("container"),
                                        id("1")
                                )
                        )
                )
                .post("/unshareable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testShareableForbiddenAccess() {
        // Create container
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("container"),
                                        id("1")
                                )
                        )
                )
                .post("/container")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create shareable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("shareable"),
                                        id(null)
                                )
                        )
                )
                .post("/shareable")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Fail to update shareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("container"),
                                        id("1")
                                )
                        )
                )
                .patch("/shareable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to set shareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("container"),
                                        id("1")
                                )
                        )
                )
                .post("/shareable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testShareablePost() {
        // Create container
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("container"),
                                        id(null)
                                )
                        )
                )
                .post("/container")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create shareable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("shareable"),
                                        id(null)
                                )
                        )
                )
                .post("/shareable")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Add shareable to container's shareables
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("shareable"),
                                        id("1")
                                )
                        )
                )
                .post("/container/1/relationships/shareables")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/container/1")
                .then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(
                        resource(
                                type("container"),
                                id("1"),
                                relationships(
                                        relation("shareables",
                                                linkage(type("shareable"), id("1"))
                                        ),
                                        relation("unshareables")
                                )
                        )).toJSON())
                );
    }

    @Test
    public void testShareablePatch() {
        // Create container
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(datum(
                        resource(
                                type("container"),
                                id(null)
                        )
                ))
                .post("/container")
                .then().statusCode(HttpStatus.SC_CREATED);

        // Create shareable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(datum(
                        resource(
                                type("shareable"),
                                id(null)
                        )
                ))
                .post("/shareable")
                .then().statusCode(HttpStatus.SC_CREATED);

        // Add shareable to container's shareables
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(datum(
                        resource(
                                type("shareable"),
                                id("1")
                        )
                ))
                .patch("/container/1/relationships/shareables")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/container/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(
                        resource(
                                type("container"),
                                id("1"),
                                relationships(
                                        relation("shareables",
                                                linkage(type("shareable"), id("1"))
                                        ),
                                        relation("unshareables")
                                )
                        )).toJSON())
                );
    }

    @Test
    public void testCreateContainerAndUnshareables() throws Exception {
        Response response = given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body("    [\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/container\",\n"
                        + "        \"value\": {\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ab\",\n"
                        + "          \"type\": \"container\",\n"
                        + "          \"relationships\": {\n"
                        + "            \"unshareables\": {\n"
                        + "              \"data\": [\n"
                        + "                {\n"
                        + "                  \"type\": \"unshareable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "                },\n"
                        + "                {\n"
                        + "                  \"type\": \"unshareable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "                }\n"
                        + "              ]\n"
                        + "            }\n"
                        + "          }\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/unshareable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"unshareable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/unshareable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"unshareable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "        }\n"
                        + "      }\n"
                        + "    ]"
                )
                .patch("/");

        response.then().statusCode(HttpStatus.SC_OK);

        ArrayNode patchJson = (ArrayNode) mapper.readTree(response.asString());

        // Should have 3 results, 1st is container, 2nd and 3rd are unshareables
        assertEquals(3, patchJson.size());
        assertEquals("container", patchJson.get(0).get("data").get("type").asText());
        assertEquals("unshareable", patchJson.get(1).get("data").get("type").asText());
        assertEquals("unshareable", patchJson.get(2).get("data").get("type").asText());

        // Container should have 2 unshareables
        assertEquals(2, patchJson.get(0).get("data").get("relationships").get("unshareables").get("data").size());
    }

    @Test
    public void testCreateContainerAndShareables() throws Exception {
        Response patchResponse = given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body("    [\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/container\",\n"
                        + "        \"value\": {\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ab\",\n"
                        + "          \"type\": \"container\",\n"
                        + "          \"relationships\": {\n"
                        + "            \"shareables\": {\n"
                        + "              \"data\": [\n"
                        + "                {\n"
                        + "                  \"type\": \"shareable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "                },\n"
                        + "                {\n"
                        + "                  \"type\": \"shareable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "                }\n"
                        + "              ]\n"
                        + "            }\n"
                        + "          }\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/shareable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"shareable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/shareable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"shareable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "        }\n"
                        + "      }\n"
                        + "    ]"
                )
                .patch("/");

        patchResponse.then().statusCode(HttpStatus.SC_OK);

        ArrayNode patchJson = (ArrayNode) mapper.readTree(patchResponse.asString());

        // Should have 3 results, 1st is container, 2nd and 3rd are shareables
        assertEquals(3, patchJson.size());
        assertEquals("container", patchJson.get(0).get("data").get("type").asText());
        assertEquals("shareable", patchJson.get(1).get("data").get("type").asText());
        assertEquals("shareable", patchJson.get(2).get("data").get("type").asText());

        // Container should have 2 shareables
        assertEquals(2, patchJson.get(0).get("data").get("relationships").get("shareables").get("data").size());
    }

    @Test
    public void addUnsharedRelationship() {
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("right"),
                                        id(null),
                                        relationships(
                                                relation("one2one", TO_ONE,
                                                        linkage(type("left"), id("1"))
                                                )
                                        )
                                )
                        )
                )
                .post("/left/1/one2many")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
    }
}
