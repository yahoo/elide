/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.yahoo.elide.endpoints.bootstrap.AbstractBookAuthorData;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Elide persistence MySQL integration test.
 */
public class BookAuthorIT extends AbstractBookAuthorData {
    private final ObjectMapper mapper = new ObjectMapper();

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
