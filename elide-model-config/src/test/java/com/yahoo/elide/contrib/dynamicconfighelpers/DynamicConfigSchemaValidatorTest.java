/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        String jsonConfig = loadHjsonFromClassPath("/validator/valid/models/security.hjson");
        assertTrue(testClass.verifySchema(Config.SECURITY, jsonConfig, "security.hjson"));
    }

    @Test
    public void testInvalidSecuritySchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/validator/invalid_schema/security_invalid.hjson");
        Exception e = assertThrows(IllegalStateException.class,
                () -> testClass.verifySchema(Config.SECURITY, jsonConfig, "security_invalid.hjson"));
        String expectedMessage = "Schema validation failed for: security_invalid.hjson\n"
                        + "object instance has properties which are not allowed by the schema: [\"cardinality\",\"description\",\"name\",\"schema$\",\"table\"]";
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testValidVariableSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/validator/valid/models/variables.hjson");
        assertTrue(testClass.verifySchema(Config.MODELVARIABLE, jsonConfig, "variables.hjson"));
    }

    @Test
    public void testInvalidVariableSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/validator/invalid_schema/variables_invalid.hjson");
        Exception e = assertThrows(IllegalStateException.class,
                () -> testClass.verifySchema(Config.MODELVARIABLE, jsonConfig, "variables.hjson"));
        String expectedMessage = "Schema validation failed for: variables.hjson\n"
                        + "object instance has properties which are not allowed by the schema: [\"schema$\"]";
        assertEquals(expectedMessage, e.getMessage());
    }

    // Table config test
    @DisplayName("Valid Table config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/validator/valid/models/tables/player_stats.hjson",
            "/validator/valid/models/tables/player_stats_extends.hjson"})
    public void testValidTableSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        String fileName = getFileName(resource);
        assertTrue(testClass.verifySchema(Config.TABLE, jsonConfig, fileName));
    }

    @DisplayName("Invalid Table config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/validator/invalid_schema/table_invalid.hjson",
            "/validator/invalid_schema/invalid_dimension_data_source.hjson",
            "/validator/invalid_schema/invalid_query_plan_classname.hjson",
            "/validator/invalid_schema/invalid_table_filter.hjson"})
    public void testInvalidTableSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        String fileName = getFileName(resource);
        Exception e = assertThrows(IllegalStateException.class,
                () -> testClass.verifySchema(Config.TABLE, jsonConfig, fileName));
        assertTrue(e.getMessage().startsWith("Schema validation failed for: " + fileName));
    }

    // DB config test
    @DisplayName("Valid DB config")
    @ParameterizedTest
    @ValueSource(strings = {
            "/validator/valid/db/sql/multiple_db_no_variables.hjson",
            "/validator/valid/db/sql/single_db.hjson"})
    public void testValidDbSchema(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        String fileName = getFileName(resource);
        assertTrue(testClass.verifySchema(Config.SQLDBConfig, jsonConfig, fileName));
    }

    @Test
    public void testInvalidDbSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/validator/invalid_schema/db_invalid.hjson");
        Exception e = assertThrows(IllegalStateException.class,
                () -> testClass.verifySchema(Config.SQLDBConfig, jsonConfig, "db_invalid.hjson"));
        String expectedMessage = "Schema validation failed for: db_invalid.hjson\n"
                        + "ECMA 262 regex \"^jdbc:[0-9A-Za-z_]+:.*$\" does not match input string \"ojdbc:mysql://localhost/testdb?serverTimezone=UTC\" at node: /dbconfigs/1/url";
        assertEquals(expectedMessage, e.getMessage());
    }

    private String loadHjsonFromClassPath(String resource) throws Exception {
        Reader reader = new InputStreamReader(
                DynamicConfigSchemaValidatorTest.class.getResourceAsStream(resource));
        return JsonValue.readHjson(reader).toString();
    }

    private String getFileName(String resource) throws Exception {
        String file = DynamicConfigSchemaValidatorTest.class.getResource(resource).getFile();
        return file.substring(file.lastIndexOf("/") + 1);
    }
}
