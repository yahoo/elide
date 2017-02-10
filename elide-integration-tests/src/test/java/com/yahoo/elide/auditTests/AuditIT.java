/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.auditTests;

import com.yahoo.elide.audit.InMemoryLogger;
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer;
import com.yahoo.elide.initialization.AuditIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.utils.JsonParser;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;

/**
 * Integration tests for audit functionality.
 */
public class AuditIT extends AbstractIntegrationTestInitializer {
    private final InMemoryLogger logger = AuditIntegrationTestApplicationResourceConfig.LOGGER;
    private final JsonParser jsonParser = new JsonParser();

    private static final String JSONAPI_CONTENT_TYPE = "application/vnd.api+json";

    public AuditIT() {
        super(AuditIntegrationTestApplicationResourceConfig.class);
    }

    @Test(priority = 0)
    public void testAuditOnCreate() throws Exception {
        String request = jsonParser.getJson("/AuditIT/createAuditEntity.req.json");
        String expected = jsonParser.getJson("/AuditIT/createAuditEntity.resp.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/auditEntity")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();

        assertEqualDocuments(actual, expected);
        Assert.assertTrue(logger.logMessages.contains("old: null\n"
                + "new: Value: test abc relationship: null"));
        Assert.assertTrue(logger.logMessages.contains("Created with value: test abc"));
    }

    @Test(priority = 1)
    public void testAuditOnUpdate() {
        String request = jsonParser.getJson("/AuditIT/createAuditEntity2.req.json");
        String expected = jsonParser.getJson("/AuditIT/createAuditEntity2.resp.json");

        String actual = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/auditEntity")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract().body().asString();

        assertEqualDocuments(actual, expected);

        request = jsonParser.getJson("/AuditIT/updateAuditEntity.req.json");

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .patch("/auditEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        Assert.assertTrue(logger.logMessages.contains("Updated relationship (for id: 1): 2"));
        Assert.assertTrue(logger.logMessages.contains("Updated value (for id: 1): updated value"));
    }

    @Test(priority = 2)
    public void testAuditWithDuplicateLineageEntry() {
        String request = jsonParser.getJson("/AuditIT/updateAuditEntityLineageDup.req.json");

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .patch("/auditEntity/2/otherEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        Assert.assertTrue(logger.logMessages.contains("Updated value (for id: 1): update id 1 through id 2"));
    }

    @Test(priority = 3)
    public void testAuditUpdateOnInverseCollection() {
        String request = jsonParser.getJson("/AuditIT/createAuditEntityInverse.req.json");

        Assert.assertFalse(logger.logMessages.contains("Inverse entities: [Value: update id 1 through id 2 relationship: 2]"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/auditEntityInverse")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        Assert.assertTrue(logger.logMessages.contains("Entity with id 1 now has inverse list [AuditEntityInverse{id=1, entities=[Value: update id 1 through id 2 relationship: 2]}]"));
        Assert.assertTrue(logger.logMessages.contains("Inverse entities: [Value: update id 1 through id 2 relationship: 2]"));

        // This message may have been added on create. Remove it so we don't get a false positive.
        // NOTE: Our internal audit loggers handle this behavior by ignoring update messages associated with
        //       creations, but this is the default behavior to provide flexibility for any use case.
        logger.logMessages.remove("Entity with id 1 now has inverse list []");
        logger.logMessages.remove("Inverse entities: []");

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\":[]}")
                .patch("/auditEntity/1/relationships/inverses")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        Assert.assertTrue(logger.logMessages.contains("Entity with id 1 now has inverse list []"));
        Assert.assertTrue(logger.logMessages.contains("Inverse entities: []"));
    }
}
