/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.auditTests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.audit.InMemoryLogger;
import com.yahoo.elide.initialization.AuditIntegrationTestApplicationResourceConfig;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import com.yahoo.elide.test.jsonapi.elements.ResourceLinkage;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for audit functionality.
 */
public class AuditIT extends IntegrationTest {
    private final InMemoryLogger logger = AuditIntegrationTestApplicationResourceConfig.LOGGER;

    public AuditIT() {
        super(AuditIntegrationTestApplicationResourceConfig.class, JsonApiEndpoint.class.getPackage().getName());
    }

    private static final Resource AUDIT_1 = resource(
            type("auditEntity"),
            id("1"),
            attributes(
                    attr("value", "test abc")
            )
    );

    private static final Resource AUDIT_1_RELATIONSHIP = resource(
            type("auditEntity"),
            id("1"),
            attributes(
                    attr("value", "updated value")
            ),
            relationships(
                    relation(
                            "otherEntity",
                            true,
                            linkage(
                                    type("auditEntity"),
                                    id("2")
                            )
                    )
            )
    );

    private static final Resource AUDIT_2 = resource(
            type("auditEntity"),
            id("2"),
            attributes(
                    attr("value", "test def")
            ),
            relationships(
                    relation(
                            "otherEntity",
                            true,
                            linkage(
                                    type("auditEntity"),
                                    id("1")
                            )
                    )
            )
    );

    @Test
    public void testAuditOnCreate() {
        String expected = datum(
                resource(
                        type("auditEntity"),
                        id("1"),
                        attributes(
                                attr("value", "test abc")
                        ),
                        relationships(
                                relation("otherEntity", (ResourceLinkage[]) null),
                                relation("inverses")
                        )
                )
        ).toJSON();

        // create auditEntity 1 and validate the created entity
        String actual = createAuditEntity(AUDIT_1);

        assertEqualDocuments(actual, expected); // document comparison is needed as the order of relationship can be different
        assertTrue(logger.logMessages.contains("old: null\n"
                + "new: Value: test abc relationship: null"));
        assertTrue(logger.logMessages.contains("Created with value: test abc"));
    }

    @Test
    public void testAuditOnUpdate() {
        String expected = datum(
                resource(
                        type("auditEntity"),
                        id("2"),
                        attributes(
                                attr("value", "test def")
                        ),
                        relationships(
                                relation(
                                        "otherEntity",
                                        linkage(
                                                type("auditEntity"),
                                                id("1")
                                        )
                                ),
                                relation("inverses")
                        )
                )
        ).toJSON();

        // create auditEntity 1
        createAuditEntity(AUDIT_1);

        // create auditEntity 2 and validate the created entity
        String actual = createAuditEntity(AUDIT_2);

        assertEqualDocuments(actual, expected); // document comparison is needed as the order of relationship can be different

        // update auditEntity 1 directly
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(AUDIT_1_RELATIONSHIP).toJSON()
                )
                .patch("/auditEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertTrue(logger.logMessages.contains("Updated relationship (for id: 1): 2"));
        assertTrue(logger.logMessages.contains("Updated value (for id: 1): updated value"));
    }

    @Test
    public void testAuditWithDuplicateLineageEntry() {
        // create auditEntity 1 and 2
        createAuditEntity(AUDIT_1);
        createAuditEntity(AUDIT_2);

        // update auditEntity 1 through the relationship of auditEntity 2
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("auditEntity"),
                                        id("1"),
                                        attributes(
                                                attr("value", "update id 1 through id 2")
                                        )
                                )
                        ).toJSON()
                )
                .patch("/auditEntity/2/otherEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertTrue(logger.logMessages.contains("Updated value (for id: 1): update id 1 through id 2"));
    }

    @Test
    public void testAuditUpdateOnInverseCollection() {
        // create auditEntity 1 and 2, update auditEntity 1 to have relationship to auditEntity 2
        createAuditEntity(AUDIT_1);
        createAuditEntity(AUDIT_2);
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(AUDIT_1_RELATIONSHIP).toJSON()
                )
                .patch("/auditEntity/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertFalse(logger.logMessages.contains("Inverse entities: [Value: updated value relationship: 2]"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("auditEntityInverse"),
                                        id("1"),
                                        relationships(
                                                relation(
                                                        "entities",
                                                        linkage(
                                                                type("auditEntity"),
                                                                id("1")
                                                        )
                                                )
                                        )
                                )
                        ).toJSON()
                )
                .post("/auditEntityInverse")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        assertTrue(logger.logMessages.contains("Entity with id 1 now has inverse list [AuditEntityInverse{id=1, entities=[Value: updated value relationship: 2]}]"));
        assertTrue(logger.logMessages.contains("Inverse entities: [Value: updated value relationship: 2]"));

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

        assertTrue(logger.logMessages.contains("Entity with id 1 now has inverse list []"));
        assertTrue(logger.logMessages.contains("Inverse entities: []"));
    }

    private String createAuditEntity(Resource auditEntity) {
        return given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(auditEntity).toJSON()
                )
                .post("/auditEntity")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .asString();
    }
}
