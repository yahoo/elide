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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStreamReader;
import java.io.Reader;

public class DynamicConfigSchemaValidatorTest {

    private DynamicConfigSchemaValidator testClass = new DynamicConfigSchemaValidator();

    // Security config test
    @DisplayName("Valid Security config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/security/valid/security.hjson",
            "/models/security.hjson",
            "/security/valid/security.json"})
    public void testValidSecuritySchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        assertTrue(testClass.verifySchema(Config.SECURITY, jsonConfig));
    }

    @DisplayName("Invalid Security config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/security/invalid/security.json",
            "/security/invalid/security.hjson"})
    public void testInvalidSecuritySchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        Exception e = assertThrows(ProcessingException.class,
                () -> testClass.verifySchema(Config.SECURITY, jsonConfig));
        assertTrue(e.getMessage().startsWith("fatal: Schema validation failed"));
    }

    // Variable config test
    @DisplayName("Valid Variable config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/variables/valid/variables.json",
            "/variables/valid/variables.hjson",
            "/models/variables.hjson",
            "/validator/valid/db/variables.hjson"})
    public void testValidVariableSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        assertTrue(testClass.verifySchema(Config.MODELVARIABLE, jsonConfig));
    }

    @DisplayName("Invalid Variable config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/variables/invalid/variables.hjson",
            "/variables/invalid/variables.json"})
    public void testInvalidVariableSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        Exception e = assertThrows(ProcessingException.class,
                () -> testClass.verifySchema(Config.MODELVARIABLE, jsonConfig));
        assertTrue(e.getMessage().startsWith("fatal: Schema validation failed"));
    }

    // Table config test
    @DisplayName("Valid Table config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/tables/valid/table.json",
            "/tables/valid/table.hjson",
            "/models/tables/table1.hjson",
            "/models/tables/table2.hjson",
            "/models_missing/tables/table1.hjson"})
    public void testValidTableSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        assertTrue(testClass.verifySchema(Config.TABLE, jsonConfig));
    }

    @DisplayName("Invalid Table config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/tables/invalid/table.json",
            "/tables/invalid/table.hjson"})
    public void testInvalidTableSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        Exception e = assertThrows(ProcessingException.class,
                () -> testClass.verifySchema(Config.TABLE, jsonConfig));
        assertTrue(e.getMessage().startsWith("fatal: Schema validation failed"));
    }

    // DB config test
    @DisplayName("Valid DB config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/configs/db/sql/db1.hjson",
            "/configs/db/sql/db2.hjson",
            "/configs/db/nonsql/db1.hjson"})
    public void testValidDbSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        assertTrue(testClass.verifySchema(Config.SQLDBConfig, jsonConfig));
    }

    @DisplayName("Invalid DB config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/invalid_db/sql/db1.hjson",
            "/invalid_db/nonsql/db1.hjson"})
    public void testInvalidDbSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
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
