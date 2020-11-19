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
                        + "[ERROR]\n"
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
                        + "[ERROR]\n"
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

    @DisplayName("Invalid Table config")
    @ParameterizedTest
    @ValueSource(strings = {"/validator/invalid_schema/table_schema_with_multiple_errors.hjson"})
    public void testInvalidTableSchemaMultipleErrors(String resource) throws Exception {
        String jsonConfig = loadHjsonFromClassPath(resource);
        String fileName = getFileName(resource);
        Exception e = assertThrows(IllegalStateException.class,
                        () -> testClass.verifySchema(Config.TABLE, jsonConfig, fileName));
        String expectedMessage = "Schema validation failed for: table_schema_with_multiple_errors.hjson\n"
                        + "[ERROR]\n"
                        + "object instance has properties which are not allowed by the schema: [\"name\"]\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/cardinality] failed to validate against schema[/properties/tables/items/properties/cardinality]. Cardinality type [Extra Large] is not allowed. Supported value is one of [Tiny, Small, Medium, Large, Huge].\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/0] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/0] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/0/cardinality] failed to validate against schema[/definitions/dimensionRef/properties/cardinality]. Cardinality type [Extra small] is not allowed. Supported value is one of [Tiny, Small, Medium, Large, Huge].\n"
                        + "        Instance[/tables/0/dimensions/0/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [id] is not allowed. Field name cannot be 'id'\n"
                        + "        Instance[/tables/0/dimensions/0/type] failed to validate against schema[/definitions/dimension/allOf/1/properties/type]. Field type [Float] is not allowed. Supported value is one of [Integer, Decimal, Money, Text, Coordinate, Boolean].\n"
                        + "    Instance[/tables/0/dimensions/0] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/0/cardinality] failed to validate against schema[/definitions/dimensionRef/properties/cardinality]. Cardinality type [Extra small] is not allowed. Supported value is one of [Tiny, Small, Medium, Large, Huge].\n"
                        + "        Instance[/tables/0/dimensions/0/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [id] is not allowed. Field name cannot be 'id'\n"
                        + "        Instance[/tables/0/dimensions/0/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [Float] is not allowed. Field type must be [Time] for any time dimension.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/1] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/1/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [_region] is not allowed. Field name must start with an alphabet and can include alaphabets, numbers and '_' only.\n"
                        + "        Instance[/tables/0/dimensions/1/tags] failed to validate against schema[/definitions/dimensionRef/properties/tags]. instance type (string) does not match any allowed primitive type (allowed: [\"array\"])\n"
                        + "        Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/dimension/allOf/1]. Either tableSource or values should be defined for a dimension, Both are not allowed.\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/1/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [_region] is not allowed. Field name must start with an alphabet and can include alaphabets, numbers and '_' only.\n"
                        + "        Instance[/tables/0/dimensions/1/tags] failed to validate against schema[/definitions/dimensionRef/properties/tags]. instance type (string) does not match any allowed primitive type (allowed: [\"array\"])\n"
                        + "        Instance[/tables/0/dimensions/1/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [Text] is not allowed. Field type must be [Time] for any time dimension.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/2] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/2] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 1 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/2/type] failed to validate against schema[/definitions/dimension/allOf/1/properties/type]. Field type [TIMEX] is not allowed. Supported value is one of [Integer, Decimal, Money, Text, Coordinate, Boolean].\n"
                        + "    Instance[/tables/0/dimensions/2] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 1 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/2/grain/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/grain/properties/type]. Grain type [Days] is not allowed. Supported value is one of [Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year].\n"
                        + "        Instance[/tables/0/dimensions/2/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [TIMEX] is not allowed. Field type must be [Time] for any time dimension.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/filterTemplate] failed to validate against schema[/properties/tables/items/properties/filterTemplate]. filterTemplate [countryIsoCode={{code}};startTime=={{start}}] is not allowed. RSQL filter Template must follow the format 'XoperatorY;XoperatorY;XoperatorY'. Here `X` must start with an alphabet and can include alaphabets, numbers and '_' only. Here `operator` must be one of [==, !=, >=, >, <, <=, =anylowercaseword=]. Here `Y` can be anything and number of `XoperatorY` can vary but must appear atleast once.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/measures/0/queryPlanResolver] failed to validate against schema[/definitions/measure/properties/queryPlanResolver]. Class Name [com.yahoo.elide.datastores.aggregation.query$DefaultQueryPlanResolver.class] is not allowed. Class name must follow the format 'X.X.X.X.class'. Here `X` must start with an alphabet and can include alaphabets, numbers and '_' only. Also, number of `X` can vary but must appear atleast once.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/name] failed to validate against schema[/properties/tables/items/properties/name]. Name [Country@10] is not allowed. Name must start with an alphabet and can include alaphabets, numbers and '_' only.";

        assertEquals(expectedMessage, e.getMessage());
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
                        + "[ERROR]\n"
                        + "Instance[/dbconfigs/1/url] failed to validate against schema[/properties/dbconfigs/items/properties/url]. ECMA 262 regex \"^jdbc:[0-9A-Za-z_]+:.*$\" does not match input string \"ojdbc:mysql://localhost/testdb?serverTimezone=UTC\"";
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
