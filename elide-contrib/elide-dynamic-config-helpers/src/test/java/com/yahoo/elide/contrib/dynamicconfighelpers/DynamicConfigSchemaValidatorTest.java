/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import org.hjson.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStreamReader;
import java.io.Reader;

public class DynamicConfigSchemaValidatorTest {

    private DynamicConfigSchemaValidator testClass = new DynamicConfigSchemaValidator();

    @Test
    public void testValidSecuritySchemas() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/schema_validation/security_valid.hjson");
        assertTrue(testClass.verifySchema(Config.SECURITY, jsonConfig));
    }

    @Test
    public void testInvalidSecuritySchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/schema_validation/security_invalid.hjson");
        Exception e = assertThrows(ProcessingException.class,
                () -> testClass.verifySchema(Config.SECURITY, jsonConfig));
        assertTrue(e.getMessage().startsWith("fatal: Schema validation failed"));
    }

    @Test
    public void testValidVariableSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/schema_validation/variables_valid.hjson");
        assertTrue(testClass.verifySchema(Config.MODELVARIABLE, jsonConfig));
    }

    @Test
    public void testInvalidVariableSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/schema_validation/variables_invalid.hjson");
        Exception e = assertThrows(ProcessingException.class,
                () -> testClass.verifySchema(Config.MODELVARIABLE, jsonConfig));
        assertTrue(e.getMessage().startsWith("fatal: Schema validation failed"));
    }

    // Table config test
    @DisplayName("Valid Table config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/schema_validation/table_valid.hjson",
            "/schema_validation/table_valid_extends.hjson"})
    public void testValidTableSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        assertTrue(testClass.verifySchema(Config.TABLE, jsonConfig));
    }

    @Test
    public void testInvalidTableSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/schema_validation/table_invalid.hjson");
        Exception e = assertThrows(ProcessingException.class,
                () -> testClass.verifySchema(Config.TABLE, jsonConfig));
        assertTrue(e.getMessage().startsWith("fatal: Schema validation failed"));
    }

    // DB config test
    @DisplayName("Valid DB config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/schema_validation/multiple_db_valid.hjson",
            "/schema_validation/db_valid.hjson"})
    public void testValidDbSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        assertTrue(testClass.verifySchema(Config.SQLDBConfig, jsonConfig));
    }

    @Test
    public void testInvalidDbSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/schema_validation/db_invalid.hjson");
        Exception e = assertThrows(ProcessingException.class,
                () -> testClass.verifySchema(Config.SQLDBConfig, jsonConfig));
        assertTrue(e.getMessage().startsWith("fatal: Schema validation failed"));
    }

    private String loadHjsonFromClassPath(String resource) throws Exception {
        Reader reader = new InputStreamReader(
                DynamicConfigSchemaValidatorTest.class.getResourceAsStream(resource));
        return JsonValue.readHjson(reader).toString();
    }
}
