/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

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
        Exception e = assertThrows(InvalidSchemaException.class,
                () -> testClass.verifySchema(Config.SECURITY, jsonConfig, "security_invalid.hjson"));
        String expectedMessage = """
                Schema validation failed for: security_invalid.hjson
                : property 'name' is not defined in the schema and the schema does not allow additional properties
                : property 'table' is not defined in the schema and the schema does not allow additional properties
                : property 'schema$' is not defined in the schema and the schema does not allow additional properties
                : property 'description' is not defined in the schema and the schema does not allow additional properties
                : property 'cardinality' is not defined in the schema and the schema does not allow additional properties""";
        assertEquals(expectedMessage.replaceAll("\n", System.lineSeparator()), e.getMessage());
    }

    @Test
    public void testValidVariableSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/validator/valid/models/variables.hjson");
        assertTrue(testClass.verifySchema(Config.MODELVARIABLE, jsonConfig, "variables.hjson"));
    }

    @Test
    public void testInvalidVariableSchema() throws Exception {
        String jsonConfig = loadHjsonFromClassPath("/validator/invalid_schema/variables_invalid.hjson");
        Exception e = assertThrows(InvalidSchemaException.class,
                () -> testClass.verifySchema(Config.MODELVARIABLE, jsonConfig, "variables.hjson"));
        String expectedMessage = """
                Schema validation failed for: variables.hjson
                /cardinality: null found, [string, number, boolean, array, object] expected
                : property 'schema$' is not defined in the schema and the schema does not allow additional properties""";
        assertEquals(expectedMessage.replaceAll("\n", System.lineSeparator()), e.getMessage());
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
        Exception e = assertThrows(InvalidSchemaException.class,
                () -> testClass.verifySchema(Config.TABLE, jsonConfig, fileName));
        assertTrue(e.getMessage().startsWith("Schema validation failed for: " + fileName));
    }

    @DisplayName("Invalid Table config")
    @ParameterizedTest
    @ValueSource(strings = {"/validator/invalid_schema/table_schema_with_multiple_errors.hjson"})
    public void testInvalidTableSchemaMultipleErrors(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        String fileName = getFileName(resource);
        Exception e = assertThrows(InvalidSchemaException.class,
                        () -> testClass.verifySchema(Config.TABLE, jsonConfig, fileName));
        String expectedMessage = """
                Schema validation failed for: table_schema_with_multiple_errors.hjson
                /tables/0/name: does not match the elideName pattern must start with an alphabetic character and can include alphabets, numbers and '_' only.
                /tables/0/filterTemplate: does not match the elideRSQLFilter pattern is not a valid RSQL filter expression. Please visit page https://elide.io/pages/guide/v5/11-graphql.html#operators for samples.
                /tables/0/cardinality: does not match the elideCardinality pattern must be one of [Tiny, Small, Medium, Large, Huge].
                /tables/0/measures/0/maker: does not match the javaClassName pattern is not a valid Java class name.
                /tables/0/measures/0: must be valid to one and only one schema, but 2 are valid with indexes '0, 1'
                /tables/0/dimensions/0: must be valid to one and only one schema, but 0 are valid
                /tables/0/dimensions/0/name: does not match the elideFieldName pattern must start with lower case alphabet and can include alphabets, numbers and '_' only and cannot be one of [id, sql]
                /tables/0/dimensions/0/cardinality: does not match the elideCardinality pattern must be one of [Tiny, Small, Medium, Large, Huge].
                /tables/0/dimensions/0/type: does not match the elideFieldType pattern must be one of [Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].
                /tables/0/dimensions/0/name: does not match the elideFieldName pattern must start with lower case alphabet and can include alphabets, numbers and '_' only and cannot be one of [id, sql]
                /tables/0/dimensions/0/cardinality: does not match the elideCardinality pattern must be one of [Tiny, Small, Medium, Large, Huge].
                /tables/0/dimensions/0/type: does not match the elideTimeFieldType pattern must be [Time] for any time dimension.
                /tables/0/dimensions/0: property 'tableSource' is not defined in the schema and the schema does not allow additional properties
                /tables/0/dimensions/1: must be valid to one and only one schema, but 0 are valid
                /tables/0/dimensions/1/name: does not match the elideFieldName pattern must start with lower case alphabet and can include alphabets, numbers and '_' only and cannot be one of [id, sql]
                /tables/0/dimensions/1/tags: string found, array expected
                /tables/0/dimensions/1: tableSource and values cannot both be defined for a dimension. Choose One or None.
                /tables/0/dimensions/1/name: does not match the elideFieldName pattern must start with lower case alphabet and can include alphabets, numbers and '_' only and cannot be one of [id, sql]
                /tables/0/dimensions/1/tags: string found, array expected
                /tables/0/dimensions/1/type: does not match the elideTimeFieldType pattern must be [Time] for any time dimension.
                /tables/0/dimensions/1: property 'values' is not defined in the schema and the schema does not allow additional properties
                /tables/0/dimensions/1: property 'tableSource' is not defined in the schema and the schema does not allow additional properties
                /tables/0/dimensions/2: must be valid to one and only one schema, but 0 are valid
                /tables/0/dimensions/2/type: does not match the elideFieldType pattern must be one of [Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].
                /tables/0/dimensions/2: property 'grains' is not defined in the schema and the schema does not allow additional properties
                /tables/0/dimensions/2/type: does not match the elideTimeFieldType pattern must be [Time] for any time dimension.
                /tables/0/dimensions/2/grains/0/type: does not match the elideGrainType pattern must be one of [Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year].
                /tables/0/arguments/0/type: does not match the elideFieldType pattern must be one of [Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].
                /tables/0/arguments/0: required property 'default' not found
                /tables/0/arguments/1/name: does not match the elideArgumentName pattern must start with an alphabetic character and can include alphabets, numbers and '_' only and cannot be 'grain'.
                /tables/0/arguments/1: required property 'default' not found
                /tables/0/arguments/2: tableSource and values cannot both be defined for an argument. Choose One or None.
                : property 'name' is not defined in the schema and the schema does not allow additional properties""";

        assertEquals(expectedMessage.replaceAll("\n", System.lineSeparator()), e.getMessage());
    }

    @DisplayName("Invalid Table config")
    @ParameterizedTest
    @ValueSource(strings = {"/validator/invalid_schema/table_schema_with_arguments.hjson"})
    public void testInvalidTableSchemaArgument(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        String fileName = getFileName(resource);
        Exception e = assertThrows(InvalidSchemaException.class,
                        () -> testClass.verifySchema(Config.TABLE, jsonConfig, fileName));
        String expectedMessage = """
                Schema validation failed for: table_schema_with_arguments.hjson
                /tables/0/filterTemplate: does not match the elideRSQLFilter pattern is not a valid RSQL filter expression. Please visit page https://elide.io/pages/guide/v5/11-graphql.html#operators for samples.
                /tables/0/cardinality: does not match the elideCardinality pattern must be one of [Tiny, Small, Medium, Large, Huge].
                /tables/0/arguments/0: tableSource and values cannot both be defined for an argument. Choose One or None.
                /tables/0: must be valid to one and only one schema, but 0 are valid
                /tables/0: required property 'sql' not found
                /tables/0: required property 'dimensions' not found
                /tables/0: required property 'maker' not found
                /tables/0: required property 'dimensions' not found
                /tables/0: required property 'dimensions' not found
                /tables/0: required property 'extend' not found
                : property 'name' is not defined in the schema and the schema does not allow additional properties""";

        assertEquals(expectedMessage.replaceAll("\n", System.lineSeparator()), e.getMessage());
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
        Exception e = assertThrows(InvalidSchemaException.class,
                () -> testClass.verifySchema(Config.SQLDBConfig, jsonConfig, "db_invalid.hjson"));
        String expectedMessage = """
                Schema validation failed for: db_invalid.hjson
                /dbconfigs/0/name: does not match the elideName pattern must start with an alphabetic character and can include alphabets, numbers and '_' only.
                /dbconfigs/0/driver: does not match the javaClassName pattern is not a valid Java class name.
                /dbconfigs/0/propertyMap/hibernate.show_sql: null found, [string, number, boolean, array, object] expected
                /dbconfigs/1/url: does not match the elideJdbcUrl pattern must start with 'jdbc:'.
                /dbconfigs/1/dialect: integer found, string expected""";
        assertEquals(expectedMessage.replaceAll("\n", System.lineSeparator()), e.getMessage());
    }

    private String loadHjsonFromClassPath(String resource) throws Exception {
        Reader reader = new InputStreamReader(
                DynamicConfigSchemaValidatorTest.class.getResourceAsStream(resource));
        return JsonValue.readHjson(reader).toString();
    }

    private String getFileName(String resource) throws Exception {
        String file = DynamicConfigSchemaValidatorTest.class.getResource(resource).getFile();
        return file.substring(file.lastIndexOf('/') + 1);
    }
}
