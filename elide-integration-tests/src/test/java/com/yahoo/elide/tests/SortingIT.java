/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.utils.JsonParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SortingIT extends IntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonParser jsonParser = new JsonParser();

    @BeforeClass
    public void setup() {
        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body("[\n"
                        + "{\n"
                        + "  \"op\": \"add\",\n"
                        + "  \"path\": \"/author\",\n"
                        + "  \"value\": {\n"
                        + "    \"id\": \"12345678-1234-1234-1234-1234567890ab\",\n"
                        + "    \"type\": \"author\",\n"
                        + "    \"attributes\": {\n"
                        + "      \"name\": \"Ernest Hemingway\"\n"
                        + "    },\n"
                        + "    \"relationships\": {\n"
                        + "      \"books\": {\n"
                        + "        \"data\": [\n"
                        + "          {\n"
                        + "            \"type\": \"book\",\n"
                        + "            \"id\": \"12345678-1234-1234-1234-1234567890ac\"\n"
                        + "          },\n"
                        + "          {\n"
                        + "            \"type\": \"book\",\n"
                        + "            \"id\": \"12345678-1234-1234-1234-1234567890ad\"\n"
                        + "          }\n"
                        + "        ]\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "},\n"
                        + "{\n"
                        + "  \"op\": \"add\",\n"
                        + "  \"path\": \"/book\",\n"
                        + "  \"value\": {\n"
                        + "    \"type\": \"book\",\n"
                        + "    \"id\": \"12345678-1234-1234-1234-1234567890ac\",\n"
                        + "    \"attributes\": {\n"
                        + "      \"title\": \"The Old Man and the Sea\",\n"
                        + "      \"genre\": \"Literary Fiction\",\n"
                        + "      \"language\": \"English\"\n"
                        + "    },\n"
                        + "    \"relationships\": {\n"
                        + "      \"publisher\": {\n"
                        + "        \"data\": {\n"
                        + "          \"type\": \"publisher\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890ae\"\n"
                        + "        }\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "},\n"
                        + "{\n"
                        + "  \"op\": \"add\",\n"
                        + "  \"path\": \"/book\",\n"
                        + "  \"value\": {\n"
                        + "    \"type\": \"book\",\n"
                        + "    \"id\": \"12345678-1234-1234-1234-1234567890ad\",\n"
                        + "    \"attributes\": {\n"
                        + "      \"title\": \"For Whom the Bell Tolls\",\n"
                        + "      \"genre\": \"Literary Fiction\",\n"
                        + "      \"language\": \"English\"\n"
                        + "    },\n"
                        + "    \"relationships\": {\n"
                        + "      \"publisher\": {\n"
                        + "        \"data\": {\n"
                        + "          \"type\": \"publisher\",\n"
                        + "          \"id\": \"12345678-1234-1234-1234-1234567890af\"\n"
                        + "        }\n"
                        + "      }\n"
                        + "    }\n"
                        + "  }\n"
                        + "},\n"
                        + "{\n"
                        + "  \"op\": \"add\",\n"
                        + "  \"path\": \"/book/12345678-1234-1234-1234-1234567890ac/publisher\",\n"
                        + "  \"value\": {\n"
                        + "    \"type\": \"publisher\",\n"
                        + "    \"id\": \"12345678-1234-1234-1234-1234567890ae\",\n"
                        + "    \"attributes\": {\n"
                        + "      \"name\": \"Default publisher\"\n"
                        + "    }\n"
                        + "  }\n"
                        + "},\n"
                        + "{\n"
                        + "  \"op\": \"add\",\n"
                        + "  \"path\": \"/book/12345678-1234-1234-1234-1234567890ad/publisher\",\n"
                        + "  \"value\": {\n"
                        + "    \"type\": \"publisher\",\n"
                        + "    \"id\": \"12345678-1234-1234-1234-1234567890af\",\n"
                        + "    \"attributes\": {\n"
                        + "      \"name\": \"Super Publisher\"\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n"
                        +"]")
                .patch("/");

        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher2.json"))
                .patch("/");

        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher3.json"))
                .patch("/");

        RestAssured
                .given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher4.json"))
                .patch("/");
    }

    @Test
    public void testSortingRootCollectionByRelationshipProperty() throws IOException {
        JsonNode result = mapper.readTree(
                RestAssured.get("/book?sort=-publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
                RestAssured.get("/book?sort=publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
    }

    @Test
    public void testSortingSubcollectionByRelationshipProperty() throws IOException {
        JsonNode result = mapper.readTree(
                RestAssured.get("/author/1/books?sort=-publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
                RestAssured.get("/author/1/books?sort=publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
    }

    @Test
    public void testSortingRootCollectionByRelationshipPropertyWithJoinFilter() throws IOException {
        JsonNode result = mapper.readTree(
                RestAssured.get("/book?filter[book.authors.name][infixi]=Hemingway&sort=-publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
                RestAssured.get("/book?filter[book.authors.name][infixi]=Hemingway&sort=publisher.name").asString());
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        Assert.assertEquals(result.get("data").size(), 2);

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
    }

    @Test
    public void testSortingByRelationshipId() throws IOException {
        JsonNode result = mapper.readTree(
                RestAssured.get("/book?sort=-publisher.id").asString());

        //We expect 8 results because publisher_id is a foreign key inside the book table.
        Assert.assertEquals(result.get("data").size(), 8);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "For Whom the Bell Tolls");

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "The Old Man and the Sea");

        result = mapper.readTree(
                RestAssured.get("/book?sort=publisher.id").asString());
        Assert.assertEquals(result.get("data").size(), 8);

        books = result.get("data");
        firstBookName = books.get(6).get("attributes").get("title").asText();
        Assert.assertEquals(firstBookName, "The Old Man and the Sea");

        secondBookName = books.get(7).get("attributes").get("title").asText();
        Assert.assertEquals(secondBookName, "For Whom the Bell Tolls");
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
                RestAssured.get("/book?sort=-id").asString());
        Assert.assertEquals(result.get("data").size(), 8);

        JsonNode books = result.get("data");
        for (int idx = 0; idx < bookTitles.size(); idx++) {
            String expectedTitle = bookTitles.get(idx);
            String actualTitle = books.get(idx).get("attributes").get("title").asText();
            Assert.assertEquals(expectedTitle, actualTitle);
        }
    }

}
