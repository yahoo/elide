/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.patchTests;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.initialization.PatchIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.utils.JsonParser;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;

/**
 * Integration tests for response code on relationship PATCH
 * <p>
 * Base on the
 * <a href = http://jsonapi.org/format/#crud-updating-relationship-responses target="_blank">JSON API docs</a>
 * relationship updates MUST return 204 unless the server has made additional modification
 * to the relationship.
 */
public class BookChapterIT extends AbstractIntegrationTestInitializer {
    private final JsonParser jsonParser = new JsonParser();

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    public BookChapterIT() {
        super(PatchIntegrationTestApplicationResourceConfig.class);
    }

    @BeforeClass
    public void setUp() {
        // create book
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookChapterIT/oliver_twist.json"))
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // create chapter 1
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookChapterIT/oliver_twist_chapter1.json"))
                .post("/chapter")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // create chapter 2
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookChapterIT/oliver_twist_chapter2.json"))
                .post("/chapter")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // create relationship: book -> chapter 1
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookChapterIT/chapter1_relationship.json"))
                .patch("/book/1/relationships/chapters")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test(priority = 1)
    public void testPatchWithoutModifyingRelationship() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookChapterIT/chapter1_relationship.json"))
                .patch("/book/1/relationships/chapters")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test(priority = 2)
    public void testPatchByModifyingRelationship() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(jsonParser.getJson("/BookChapterIT/chapter2_relationship.json"))
                .patch("/book/1/relationships/chapters")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
