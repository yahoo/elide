/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.utils.JsonParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SortingIT extends IntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonParser jsonParser = new JsonParser();

    @BeforeEach
    public void setup() {

        given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher1.json"))
                .patch("/");


        given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher2.json"))
                .patch("/");

        given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher3.json"))
                .patch("/");


        given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher4.json"))
                .patch("/");
    }

    @Test
    public void testSortingRootCollectionByRelationshipProperty() throws IOException {
        JsonNode result = mapper.readTree(
                get("/book?sort=-publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());


        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", firstBookName);

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", secondBookName);

        result = mapper.readTree(
                get("/book?sort=publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", firstBookName);

        secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", secondBookName);
    }

    @Test
    public void testSortingSubcollectionByRelationshipProperty() throws IOException {
        JsonNode result = mapper.readTree(
                get("/author/1/books?sort=-publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", firstBookName);

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", secondBookName);

        result = mapper.readTree(
                get("/author/1/books?sort=publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", firstBookName);

        secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", secondBookName);
    }

    @Test
    public void testSortingRootCollectionByRelationshipPropertyWithJoinFilter() throws IOException {
        JsonNode result = mapper.readTree(
                get("/book?filter[book.authors.name][infixi]=Hemingway&sort=-publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", firstBookName);

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", secondBookName);

        result = mapper.readTree(
                get("/book?filter[book.authors.name][infixi]=Hemingway&sort=publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", firstBookName);

        secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", secondBookName);
    }

    @Test
    public void testSortingByRelationshipId() throws IOException {
        JsonNode result = mapper.readTree(
                get("/book?sort=-publisher.id").asString());

        //We expect 8 results because publisher_id is a foreign key inside the book table.
        assertEquals(8, result.get("data").size());

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", firstBookName);

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", secondBookName);

        result = mapper.readTree(
                get("/book?sort=publisher.id").asString());
        assertEquals(8, result.get("data").size());

        books = result.get("data");
        firstBookName = books.get(6).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", firstBookName);

        secondBookName = books.get(7).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", secondBookName);
    }

    @Test
    public void testSortingById() throws IOException {
        List<String> bookTitles = Arrays.asList(
                "Life with Null Ned 2",
                "Life with Null Ned",
                "The Roman Republic",
                "Foundation",
                "Enders Shadow",
                "Enders Game",
                "For Whom the Bell Tolls",
                "The Old Man and the Sea"
        );

        JsonNode result = mapper.readTree(
                get("/book?sort=-id").asString());
        assertEquals(8, result.get("data").size());

        JsonNode books = result.get("data");
        for (int idx = 0; idx < bookTitles.size(); idx++) {
            String expectedTitle = bookTitles.get(idx);
            String actualTitle = books.get(idx).get("attributes").get("title").asText();
            assertEquals(expectedTitle, actualTitle);
        }
    }
}
