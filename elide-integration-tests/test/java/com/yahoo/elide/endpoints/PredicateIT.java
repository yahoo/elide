/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import com.yahoo.elide.endpoints.bootstrap.AbstractBookAuthorData;
import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;

/**
 * Integration test for predicates.
 */
public class PredicateIT extends AbstractBookAuthorData {
    @Test
    public void testPredicateWithIncludeLiteraryFiction() throws IOException {
        String expected = getJson("/PredicateIT/testPredicateWithIncludeLiteraryFiction.json");
        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .param("include", "authors")
                .param("filter[root.book.genre]", "Literary Fiction")
                .get("/book").then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertEqualDocuments(response, expected);
    }

    @Test
    public void testPredicateWithIncludeScienceFiction() throws IOException {
        String expected = getJson("/PredicateIT/testPredicateWithIncludeScienceFiction.json");
        String response = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .param("include", "authors")
                .param("filter[root.book.genre]", "Science Fiction")
                .get("/book").then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        assertEqualDocuments(response, expected);
    }
}
