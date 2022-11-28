/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.utils.JsonParser;
import com.yahoo.elide.initialization.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SortingIT extends IntegrationTest {
    private final JsonParser jsonParser = new JsonParser();

    @BeforeEach
    public void setup() {

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher1.json"))
                .patch("/");


        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher2.json"))
                .patch("/");

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher3.json"))
                .patch("/");


        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(jsonParser.getJson("/SortingIT/addAuthorBookPublisher4.json"))
                .patch("/");
    }

    @Test
    @Tag("skipInMemory")
    public void testSortingRootCollectionByRelationshipProperty() throws IOException {
        JsonNode result = getAsNode("/book?sort=-publisher.name");
        int size = result.get("data").size();
        assertEquals(8, size);

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", firstBookName);

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", secondBookName);

        result = getAsNode("/book?sort=publisher.name");
        size = result.get("data").size();
        assertEquals(8, size);

        books = result.get("data");
        firstBookName = books.get(size - 2).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", firstBookName);

        secondBookName = books.get(size - 1).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", secondBookName);
    }

    @Test
    public void testSortingSubcollectionByRelationshipProperty() throws IOException {
        JsonNode result = getAsNode("/author/1/books?sort=-publisher.name");
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", firstBookName);

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", secondBookName);

        result = getAsNode("/author/1/books?sort=publisher.name");
        //We expect 2 results because the Hibernate does an inner join between book & publisher
        assertEquals(2, result.get("data").size());

        books = result.get("data");
        firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", firstBookName);

        secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", secondBookName);
    }

    @Test
    @Tag("skipInMemory")
    public void testSortingRootCollectionByRelationshipPropertyWithJoinFilterAndPagination() throws IOException {
        final JsonNode result = getAsNode("/book?filter[book.authors.name][infixi]=Hemingway&sort=-publisher.name", HttpStatus.SC_BAD_REQUEST);
        assertNotNull(result.get("errors"));
    }

    @Test
    public void testSortingByRelationshipId() throws IOException {
        JsonNode result = getAsNode("/book?sort=-publisher.id");

        //We expect 8 results because publisher_id is a foreign key inside the book table.
        assertEquals(8, result.get("data").size());

        JsonNode books = result.get("data");
        String firstBookName = books.get(0).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", firstBookName);

        String secondBookName = books.get(1).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", secondBookName);

        result = getAsNode("/book?sort=publisher.id");
        assertEquals(8, result.get("data").size());

        books = result.get("data");
        firstBookName = books.get(6).get("attributes").get("title").asText();
        assertEquals("The Old Man and the Sea", firstBookName);

        secondBookName = books.get(7).get("attributes").get("title").asText();
        assertEquals("For Whom the Bell Tolls", secondBookName);
    }

    @Test
    public void testSortingByComplexAttribute() throws IOException {
        List<Double> bookPrices = Arrays.asList(10.0, 9.0);

        JsonNode result = getAsNode("/book?filter[book]=price=isnull=false&sort=-price.total");
        assertEquals(2, result.get("data").size());

        JsonNode books = result.get("data");
        for (int idx = 0; idx < bookPrices.size(); idx++) {
            Double expectedPrice = bookPrices.get(idx);
            Double actualPrice = books.get(idx).get("attributes").get("price").get("total").asDouble();
            assertEquals(expectedPrice, actualPrice);
        }
    }

    @Test
    public void testSortingByNestedComplexAttribute() throws IOException {
        List<String> isoCodes = Arrays.asList("AED", "USD");

        JsonNode result = getAsNode("/book?filter[book]=price=isnull=false&sort=price.currency.isoCode");
        assertEquals(2, result.get("data").size());

        JsonNode books = result.get("data");
        for (int idx = 0; idx < isoCodes.size(); idx++) {
            String expectedCode = isoCodes.get(idx);
            String actualCode = books.get(idx).get("attributes").get("price").get("currency").get("isoCode").asText();
            assertEquals(expectedCode, actualCode);
        }
    }

    @Test
    public void testSortingSubcollectionByNestedComplexAttribute() throws IOException {
        List<String> isoCodes = Arrays.asList("AED", "USD");

        JsonNode result = getAsNode("/author/1/books?filter[book]=price=isnull=false&sort=price.currency.isoCode");
        assertEquals(2, result.get("data").size());

        JsonNode books = result.get("data");
        for (int idx = 0; idx < isoCodes.size(); idx++) {
            String expectedCode = isoCodes.get(idx);
            String actualCode = books.get(idx).get("attributes").get("price").get("currency").get("isoCode").asText();
            assertEquals(expectedCode, actualCode);
        }
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

        JsonNode result = getAsNode("/book?sort=-id");
        assertEquals(8, result.get("data").size());

        JsonNode books = result.get("data");
        for (int idx = 0; idx < bookTitles.size(); idx++) {
            String expectedTitle = bookTitles.get(idx);
            String actualTitle = books.get(idx).get("attributes").get("title").asText();
            assertEquals(expectedTitle, actualTitle);
        }
    }

    @Test
    public void testRootCollectionByNullRelationshipProperty() {
        // Test whether all book records are received
        when()
                .get("/book?sort=publisher.editor.lastName")
                .then()
                .body("data", hasSize(8))
        ;
        when()
                .get("/book?sort=-publisher.editor.lastName")
                .then()
                .body("data", hasSize(8))
        ;

    }

    @Test
    public void testSubcollectionByNullRelationshipProperty() {
        when()
                .get("/author/1/books?sort=publisher.editor.lastName")
                .then()
                .body("data", hasSize(2))
        ;
        when()
                .get("/author/1/books?sort=-publisher.editor.lastName")
                .then()
                .body("data", hasSize(2))
        ;
    }

    /**
     * Tests a computed relationship sort.
     */
    @Test
    void testSortByComputedRelationship() {
        given()
                .get("/book?sort=-editor.firstName")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data[0].relationships.editor.data.id", equalTo("1"));
    }

    /**
     * Tests a computed attribute sort.
     */
    @Test
    void testSortByComputedAttribute() {
        given()
                .get("/editor?sort=-fullName")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data[0].attributes.fullName", equalTo("John Doe"));
    }
}
