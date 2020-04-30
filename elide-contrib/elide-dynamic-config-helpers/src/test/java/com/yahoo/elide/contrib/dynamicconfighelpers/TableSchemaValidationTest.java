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
public class TableSchemaValidationTest extends SchemaTest {

    private final JsonSchema schema;

    public TableSchemaValidationTest() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        schema = factory.getJsonSchema("resource:/elideTableSchema.json");
    }

    @Test
    public void testValidTableSchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/table/valid_table.json");
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInvalidTableSchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/table/invalid_table.json");
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }

    @Test
    public void testValidTableHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/table/valid_table.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInvalidTableHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/table/invalid_table.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }
}
