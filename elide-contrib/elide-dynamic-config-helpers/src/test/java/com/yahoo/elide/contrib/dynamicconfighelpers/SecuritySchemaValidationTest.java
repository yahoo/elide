/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.junit.jupiter.api.Test;

/**
 * Security Schema functional test.
 */
public class SecuritySchemaValidationTest extends SchemaTest {

    private final JsonSchema schema;

    public SecuritySchemaValidationTest() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        schema = factory.getJsonSchema("resource:/elideSecuritySchema.json");
    }

    @Test
    public void testValidSecuritySchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/valid/security.json");
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInValidSecuritySchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/invalid/security.json");
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }

    @Test
    public void testValidSecurityHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/valid/security.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInvalidSecurityHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/invalid/security.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }

    @Test
    public void testModelecurityHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/models/security.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }
}
