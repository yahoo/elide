/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BookAuthorIT extends IntegrationTest {

    private static final String ATTRIBUTES = "attributes";
    private static final String RELATIONSHIPS = "relationships";
    private static final String INCLUDED = "included";

    private static final Resource HEMINGWAY = resource(
            type("author"),
            attributes(
                    attr("name", "Ernest Hemingway")
            )
    );

    private static final Resource THE_OLD_MAN_AND_THE_SEA = resource(
            type("book"),
            attributes(
                    attr("title", "The Old Man and the Sea"),
                    attr("genre", "Literary Fiction"),
                    attr("language", "English")
            )
    );

    private static final Resource HEMINGWAY_RELATIONSHIP = resource(
            type("author"),
            id(1)
    );

    private static final Resource ORSON_SCOTT_CARD = resource(
            type("author"),
            attributes(
                    attr("name", "Orson Scott Card")
            )
    );

    private static final Resource ENDERS_GAME = resource(
            type("book"),
            attributes(
                    attr("title", "Ender's Game"),
                    attr("genre", "Science Fiction"),
                    attr("language", "English")
            )
    );

    private static final Resource ORSON_RELATIONSHIP = resource(
            type("author"),
            id(2)
    );

    private static final Resource FOR_WHOM_THE_BELL_TOLLS = resource(
            type("book"),
            attributes(
                    attr("title", "For Whom the Bell Tolls"),
                    attr("genre", "Literary Fiction"),
                    attr("language", "English")
            )
    );

    @BeforeEach
    public void setup() {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());

        // Create Author: Ernest Hemingway
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(HEMINGWAY).toJSON()
                )
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: The Old Man and the Sea
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(THE_OLD_MAN_AND_THE_SEA).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> The Old Man and the Sea
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(HEMINGWAY_RELATIONSHIP).toJSON()
                )
                .patch("/book/1/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Author: Orson Scott Card
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(ORSON_SCOTT_CARD).toJSON()
                )
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: Ender's Game
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(ENDERS_GAME).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Orson Scott Card -> Ender's Game
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(ORSON_RELATIONSHIP).toJSON()
                )
                .patch("/book/2/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Book: For Whom the Bell Tolls
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(FOR_WHOM_THE_BELL_TOLLS).toJSON()
                )
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> For Whom the Bell Tolls
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(HEMINGWAY_RELATIONSHIP).toJSON()
                )
                .patch("/book/3/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSparseSingleDataFieldValue() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("include", "authors")
                        .param("fields[book]", "title")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            assertFalse(bookNode.has(RELATIONSHIPS));

            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertEquals(1, attributes.size());
            assertTrue(attributes.has("title"));
        }

        assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            assertFalse(include.has(ATTRIBUTES));
            assertFalse(include.has(RELATIONSHIPS));
        }
    }

    @Test
    public void testSparseTwoDataFieldValuesNoIncludes() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("fields[book]", "title,language")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            assertFalse(bookNode.has(RELATIONSHIPS));

            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertEquals(2, attributes.size());
            assertTrue(attributes.has("title"));
            assertTrue(attributes.has("language"));
        }

        assertFalse(responseBody.has(INCLUDED));
    }

    @Test
    public void testSparseNoFilters() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("include", "authors")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertTrue(attributes.has("title"));
            assertTrue(attributes.has("language"));
            assertTrue(attributes.has("genre"));

            assertTrue(bookNode.has(RELATIONSHIPS));
            JsonNode relationships = bookNode.get(RELATIONSHIPS);
            assertTrue(relationships.has("authors"));
        }

        assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            assertTrue(include.has(ATTRIBUTES));
            JsonNode attributes = include.get(ATTRIBUTES);
            assertTrue(attributes.has("name"));

            assertTrue(include.has(RELATIONSHIPS));
            JsonNode relationships = include.get(RELATIONSHIPS);
            assertTrue(relationships.has("books"));
        }
    }

    @Test
    public void testTwoSparseFieldFilters() throws Exception {
        JsonNode responseBody = mapper.readTree(
                given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("include", "authors")
                        .param("fields[book]", "title,genre,authors")
                        .param("fields[author]", "name")
                        .get("/book")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().body().asString());

        assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            assertTrue(bookNode.has(ATTRIBUTES));
            JsonNode attributes = bookNode.get(ATTRIBUTES);
            assertEquals(2, attributes.size());
            assertTrue(attributes.has("title"));
            assertTrue(attributes.has("genre"));

            assertTrue(bookNode.has(RELATIONSHIPS));
            JsonNode relationships = bookNode.get(RELATIONSHIPS);
            assertTrue(relationships.has("authors"));
        }

        assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            assertTrue(include.has(ATTRIBUTES));
            JsonNode attributes = include.get(ATTRIBUTES);
            assertTrue(attributes.has("name"));

            assertFalse(include.has(RELATIONSHIPS));
        }
    }
}
