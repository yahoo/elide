/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.hjson.JsonValue;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Security Schema functional test.
 */
public class SecuritySchemaValidationTest {

    private final JsonSchema schema;

    public SecuritySchemaValidationTest() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        schema = factory.getJsonSchema("resource:/elideSecuritySchema.json");
    }

    @Test
    public void testValidSecuritySchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/valid_security.json");
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInValidSecuritySchema() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/invalid_security.json");
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }

    @Test
    public void testValidSecurityHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/valid_security.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertTrue(results.isSuccess());
    }

    @Test
    public void testInvalidSecurityHJson() throws Exception {
        JsonNode testNode = loadJsonFromClasspath("/security/invalid_security.hjson", true);
        ProcessingReport results = schema.validate(testNode);
        assertFalse(results.isSuccess());
    }

    private InputStream loadStreamFromClasspath(String resource) throws Exception {
        return SecuritySchemaValidationTest.class.getResourceAsStream(resource);
    }

    private Reader loadReaderFromClasspath(String resource) throws Exception {
        return new InputStreamReader(loadStreamFromClasspath(resource));
    }

    private JsonNode loadJsonFromClasspath(String resource, boolean translate) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        Reader reader = loadReaderFromClasspath(resource);

        if (translate) {
            String jsonText = JsonValue.readHjson(reader).toString();
            return objectMapper.readTree(jsonText);
        }

        return objectMapper.readTree(reader);
    }

    private JsonNode loadJsonFromClasspath(String resource) throws Exception {
        return loadJsonFromClasspath(resource, false);
    }
}
