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
public class VariableSchemaValidationTest extends SchemaTest {

    private final JsonSchema schema;

    public VariableSchemaValidationTest() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        schema = factory.getJsonSchema("resource:/elideVariableSchema.json");
    }

    @Test
    public void testValidVariableSchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/variables/valid/variables.json");
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInValidVariableSchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/variables/invalid/variables.json");
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }

    @Test
    public void testValidVariableHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/variables/valid/variables.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInvalidVariableHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/variables/invalid/variables.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }

    @Test
    public void testModelsVariableHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/models/variables.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }
}
