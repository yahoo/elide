/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.hibernate.AHibernateTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Elide persistence MySQL integration test.
 */
public class BookAuthorIT extends AHibernateTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test(priority = -1)
    public void setup() throws IOException {
        // Create Author: Ernest Hemingway
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/ernest_hemingway.json"))
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: The Old Man and the Sea
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/the_old_man_and_the_sea.json"))
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> The Old Man and the Sea
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/ernest_hemingway_relationship.json"))
                .patch("/book/1/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Author: Orson Scott Card
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/orson_scott_card.json"))
                .post("/author")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Book: Ender's Game
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/enders_game.json"))
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Orson Scott Card -> Ender's Game
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/orson_scott_card_relationship.json"))
                .patch("/book/2/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Create Book: For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/for_whom_the_bell_tolls.json"))
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Create Relationship: Ernest Hemingway -> For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/BookAuthorIT/ernest_hemingway_relationship.json"))
                .patch("/book/3/relationships/authors")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSparseSingleDataFieldValue() throws Exception {
        JsonNode responseBody = mapper.readTree(
                RestAssured
                        .given()
                        .contentType("application/vnd.api+json")
                        .accept("application/vnd.api+json")
                        .param("include", "authors")
                        .param("fields[book]", "title")
                        .get("/book").asString());

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has("attributes"));
            Assert.assertFalse(bookNode.has("relationships"));

            JsonNode attributes = bookNode.get("attributes");
            Assert.assertEquals(attributes.size(), 1);
            Assert.assertTrue(attributes.has("title"));
        }

        Assert.assertTrue(responseBody.has("included"));

        for (JsonNode include : responseBody.get("included")) {
            Assert.assertFalse(include.has("attributes"));
            Assert.assertFalse(include.has("relationships"));
        }
    }

    @Test
    public void testSparseTwoDataFieldValuesNoIncludes() throws Exception {
        JsonNode responseBody = mapper.readTree(
                RestAssured
                        .given()
                        .contentType("application/vnd.api+json")
                        .accept("application/vnd.api+json")
                        .param("fields[book]", "title,language")
                        .get("/book").asString());

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has("attributes"));
            Assert.assertFalse(bookNode.has("relationships"));

            JsonNode attributes = bookNode.get("attributes");
            Assert.assertEquals(attributes.size(), 2);
            Assert.assertTrue(attributes.has("title"));
            Assert.assertTrue(attributes.has("language"));
        }

        Assert.assertFalse(responseBody.has("included"));
    }

    @Test
    public void testSparseNoFilters() throws Exception {
        JsonNode responseBody = mapper.readTree(
                RestAssured
                        .given()
                        .contentType("application/vnd.api+json")
                        .accept("application/vnd.api+json")
                        .param("include", "authors")
                        .get("/book").asString());

        System.out.println(responseBody);

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has("attributes"));
            JsonNode attributes = bookNode.get("attributes");
            Assert.assertTrue(attributes.has("title"));
            Assert.assertTrue(attributes.has("language"));
            Assert.assertTrue(attributes.has("genre"));

            Assert.assertTrue(bookNode.has("relationships"));
            JsonNode relationships = bookNode.get("relationships");
            Assert.assertTrue(relationships.has("authors"));
        }

        Assert.assertTrue(responseBody.has("included"));

        for (JsonNode include : responseBody.get("included")) {
            Assert.assertTrue(include.has("attributes"));
            JsonNode attributes = include.get("attributes");
            Assert.assertTrue(attributes.has("name"));

            Assert.assertTrue(include.has("relationships"));
            JsonNode relationships = include.get("relationships");
            Assert.assertTrue(relationships.has("books"));
        }
    }

    @Test
    public void testTwoSparseFieldFilters() throws Exception {
        JsonNode responseBody = mapper.readTree(
                RestAssured
                        .given()
                        .contentType("application/vnd.api+json")
                        .accept("application/vnd.api+json")
                        .param("include", "authors")
                        .param("fields[book]", "title,genre,authors")
                        .param("fields[author]", "name")
                        .get("/book").asString());

        Assert.assertTrue(responseBody.has("data"));

        for (JsonNode bookNode : responseBody.get("data")) {
            Assert.assertTrue(bookNode.has("attributes"));
            JsonNode attributes = bookNode.get("attributes");
            Assert.assertEquals(attributes.size(), 2);
            Assert.assertTrue(attributes.has("title"));
            Assert.assertTrue(attributes.has("genre"));

            Assert.assertTrue(bookNode.has("relationships"));
            JsonNode relationships = bookNode.get("relationships");
            Assert.assertTrue(relationships.has("authors"));
        }

        Assert.assertTrue(responseBody.has("included"));

        for (JsonNode include : responseBody.get("included")) {
            Assert.assertTrue(include.has("attributes"));
            JsonNode attributes = include.get("attributes");
            Assert.assertTrue(attributes.has("name"));

            Assert.assertFalse(include.has("relationships"));
        }
    }
}
