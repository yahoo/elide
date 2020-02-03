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
 * @NonTransferable annotation integration tests
 */
class TransferableIT extends IntegrationTest {
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
    public void testNonTransferableForbiddenAccess() {
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

        // Create untransferable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("untransferable"),
                                        id(null)
                                )
                        )
                )
                .post("/untransferable")
                .then().statusCode(HttpStatus.SC_CREATED);

        // Fail to add untransferable to container's untransferables (untransferable is not transferable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("untransferable"),
                                        id("1")
                                )
                        )
                )
                .patch("/container/1/relationships/untransferables")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to replace container's untransferables collection (untransferable is not transferable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("untransferable"),
                                        id("1")
                                )
                        )
                )
                .post("/container/1/relationships/untransferables")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to update untransferables's container (container is not transferable)
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
                .patch("/untransferable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to set untransferable's container (container is not transferable)
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
                .post("/untransferable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testTransferableForbiddenAccess() {
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

        // Create transferable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("transferable"),
                                        id(null)
                                )
                        )
                )
                .post("/transferable")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Fail to update transferable's container (container is not transferable)
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
                .patch("/transferable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Fail to set transferable's container (container is not transferable)
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
                .post("/transferable/1/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testTransferablePost() {
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

        // Create transferable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("transferable"),
                                        id(null)
                                )
                        )
                )
                .post("/transferable")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Add transferable to container's transferables
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(
                        datum(
                                resource(
                                        type("transferable"),
                                        id("1")
                                )
                        )
                )
                .post("/container/1/relationships/transferables")
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
                                        relation("transferables",
                                                linkage(type("transferable"), id("1"))
                                        ),
                                        relation("untransferables")
                                )
                        )).toJSON())
                );
    }

    @Test
    public void testTransferablePatch() {
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

        // Create transferable
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(datum(
                        resource(
                                type("transferable"),
                                id(null)
                        )
                ))
                .post("/transferable")
                .then().statusCode(HttpStatus.SC_CREATED);

        // Add transferable to container's transferables
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(datum(
                        resource(
                                type("transferable"),
                                id("1")
                        )
                ))
                .patch("/container/1/relationships/transferables")
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
                                        relation("transferables",
                                                linkage(type("transferable"), id("1"))
                                        ),
                                        relation("untransferables")
                                )
                        )).toJSON())
                );
    }

    @Test
    public void testCreateContainerAndNonTransferable() throws Exception {
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
                        + "            \"untransferables\": {\n"
                        + "              \"data\": [\n"
                        + "                {\n"
                        + "                  \"type\": \"untransferable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "                },\n"
                        + "                {\n"
                        + "                  \"type\": \"untransferable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "                }\n"
                        + "              ]\n"
                        + "            }\n"
                        + "          }\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/untransferable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"untransferable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/untransferable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"untransferable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "        }\n"
                        + "      }\n"
                        + "    ]"
                )
                .patch("/");

        response.then().statusCode(HttpStatus.SC_OK);

        ArrayNode patchJson = (ArrayNode) mapper.readTree(response.asString());

        // Should have 3 results, 1st is container, 2nd and 3rd are untransferables
        assertEquals(3, patchJson.size());
        assertEquals("container", patchJson.get(0).get("data").get("type").asText());
        assertEquals("untransferable", patchJson.get(1).get("data").get("type").asText());
        assertEquals("untransferable", patchJson.get(2).get("data").get("type").asText());

        // Container should have 2 untransferables
        assertEquals(2, patchJson.get(0).get("data").get("relationships").get("untransferables").get("data").size());
    }

    @Test
    public void testCreateContainerAndTransferables() throws Exception {
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
                        + "            \"transferables\": {\n"
                        + "              \"data\": [\n"
                        + "                {\n"
                        + "                  \"type\": \"transferable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "                },\n"
                        + "                {\n"
                        + "                  \"type\": \"transferable\",\n"
                        + "                  \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "                }\n"
                        + "              ]\n"
                        + "            }\n"
                        + "          }\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/transferable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"transferable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "        }\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"op\": \"add\",\n"
                        + "        \"path\": \"/transferable\",\n"
                        + "        \"value\": {\n"
                        + "          \"type\": \"transferable\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "        }\n"
                        + "      }\n"
                        + "    ]"
                )
                .patch("/");

        patchResponse.then().statusCode(HttpStatus.SC_OK);

        ArrayNode patchJson = (ArrayNode) mapper.readTree(patchResponse.asString());

        // Should have 3 results, 1st is container, 2nd and 3rd are transferables
        assertEquals(3, patchJson.size());
        assertEquals("container", patchJson.get(0).get("data").get("type").asText());
        assertEquals("transferable", patchJson.get(1).get("data").get("type").asText());
        assertEquals("transferable", patchJson.get(2).get("data").get("type").asText());

        // Container should have 2 transferables
        assertEquals(2, patchJson.get(0).get("data").get("relationships").get("transferables").get("data").size());
    }

    @Test
    public void addNonTransferableRelationship() {
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
