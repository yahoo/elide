/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.utils.JsonParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class BookAuthorIT extends AbstractIntegrationTestInitializer {
    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    private static final String ATTRIBUTES = "attributes";
    private static final String RELATIONSHIPS = "relationships";
    private static final String INCLUDED = "included";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonParser jsonParser = new JsonParser();

    @Test
    public void setup() throws IOException {
        // Create Author: Ernest Hemingway
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/ernest_hemingway.json"))
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: The Old Man and the Sea
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/the_old_man_and_the_sea.json"))
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> The Old Man and the Sea
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/ernest_hemingway_relationship.json"))
                .patch("/book/1/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Author: Orson Scott Card
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/orson_scott_card.json"))
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: Ender's Game
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/enders_game.json"))
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Orson Scott Card -> Ender's Game
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/orson_scott_card_relationship.json"))
                .patch("/book/2/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Book: For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/for_whom_the_bell_tolls.json"))
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookAuthorIT/ernest_hemingway_relationship.json"))
                .patch("/book/3/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSparseSingleDataFieldValue() throws Exception {
        JsonNode responseBody = objectMapper.readTree(
                RestAssured
                        .given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("include", "authors")
                        .param("fields[book]", "title")
                        .get("/book").asString());

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has(ATTRIBUTES));
            Assert.assertFalse(bookNode.has(RELATIONSHIPS));

            JsonNode attributes = bookNode.get(ATTRIBUTES);
            Assert.assertEquals(attributes.size(), 1);
            Assert.assertTrue(attributes.has("title"));
        }

        Assert.assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            Assert.assertFalse(include.has(ATTRIBUTES));
            Assert.assertFalse(include.has(RELATIONSHIPS));
        }
    }

    @Test
    public void testSparseTwoDataFieldValuesNoIncludes() throws Exception {
        JsonNode responseBody = objectMapper.readTree(
                RestAssured
                        .given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("fields[book]", "title,language")
                        .get("/book").asString());

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has(ATTRIBUTES));
            Assert.assertFalse(bookNode.has(RELATIONSHIPS));

            JsonNode attributes = bookNode.get(ATTRIBUTES);
            Assert.assertEquals(attributes.size(), 2);
            Assert.assertTrue(attributes.has("title"));
            Assert.assertTrue(attributes.has("language"));
        }

        Assert.assertFalse(responseBody.has(INCLUDED));
    }

    @Test
    public void testSparseNoFilters() throws Exception {
        JsonNode responseBody = objectMapper.readTree(
                RestAssured
                        .given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("include", "authors")
                        .get("/book").asString());

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has(ATTRIBUTES));
            JsonNode attributes = bookNode.get(ATTRIBUTES);
            Assert.assertTrue(attributes.has("title"));
            Assert.assertTrue(attributes.has("language"));
            Assert.assertTrue(attributes.has("genre"));

            Assert.assertTrue(bookNode.has(RELATIONSHIPS));
            JsonNode relationships = bookNode.get(RELATIONSHIPS);
            Assert.assertTrue(relationships.has("authors"));
        }

        Assert.assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            Assert.assertTrue(include.has(ATTRIBUTES));
            JsonNode attributes = include.get(ATTRIBUTES);
            Assert.assertTrue(attributes.has("name"));

            Assert.assertTrue(include.has(RELATIONSHIPS));
            JsonNode relationships = include.get(RELATIONSHIPS);
            Assert.assertTrue(relationships.has("books"));
        }
    }

    @Test
    public void testTwoSparseFieldFilters() throws Exception {
        JsonNode responseBody = objectMapper.readTree(
                RestAssured
                        .given()
                        .contentType(JSONAPI_CONTENT_TYPE)
                        .accept(JSONAPI_CONTENT_TYPE)
                        .param("include", "authors")
                        .param("fields[book]", "title,genre,authors")
                        .param("fields[author]", "name")
                        .get("/book").asString());

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has(ATTRIBUTES));
            JsonNode attributes = bookNode.get(ATTRIBUTES);
            Assert.assertEquals(attributes.size(), 2);
            Assert.assertTrue(attributes.has("title"));
            Assert.assertTrue(attributes.has("genre"));

            Assert.assertTrue(bookNode.has(RELATIONSHIPS));
            JsonNode relationships = bookNode.get(RELATIONSHIPS);
            Assert.assertTrue(relationships.has("authors"));
        }

        Assert.assertTrue(responseBody.has(INCLUDED));

        for (JsonNode include : responseBody.get(INCLUDED)) {
            Assert.assertTrue(include.has(ATTRIBUTES));
            JsonNode attributes = include.get(ATTRIBUTES);
            Assert.assertTrue(attributes.has("name"));

            Assert.assertFalse(include.has(RELATIONSHIPS));
        }
    }
}
