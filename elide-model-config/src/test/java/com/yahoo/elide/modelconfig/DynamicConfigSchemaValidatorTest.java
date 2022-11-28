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
                        + "object instance has properties which are not allowed by the schema: [\"schema$\"]\n"
                        + "[ERROR]\n"
                        + "Instance[/cardinality] failed to validate against schema[/patternProperties/^([A-Za-z0-9_]+[.]?)+$]. instance type (null) does not match any allowed primitive type (allowed: [\"array\",\"boolean\",\"integer\",\"number\",\"object\",\"string\"])";
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
                        + "Instance[/tables/0/arguments/0] failed to validate against schema[/definitions/argument]. object has missing required properties ([\"default\"])\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/arguments/0/type] failed to validate against schema[/definitions/argument/properties/type]. Field type [Number] is not allowed. Supported value is one of [Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/arguments/1] failed to validate against schema[/definitions/argument]. object has missing required properties ([\"default\"])\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/arguments/1/name] failed to validate against schema[/definitions/argument/properties/name]. Argument name [Grain] is not allowed. Argument name cannot be 'grain'.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/arguments/2] failed to validate against schema[/definitions/argument]. tableSource and values cannot both be defined for an argument. Choose One or None.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/cardinality] failed to validate against schema[/properties/tables/items/properties/cardinality]. Cardinality type [Extra Large] is not allowed. Supported value is one of [Tiny, Small, Medium, Large, Huge].\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/0] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/0] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/0/cardinality] failed to validate against schema[/definitions/dimensionRef/properties/cardinality]. Cardinality type [Extra small] is not allowed. Supported value is one of [Tiny, Small, Medium, Large, Huge].\n"
                        + "        Instance[/tables/0/dimensions/0/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [id] is not allowed. Field name cannot be one of [id, sql]\n"
                        + "        Instance[/tables/0/dimensions/0/type] failed to validate against schema[/definitions/dimension/allOf/1/properties/type]. Field type [Float] is not allowed. Supported value is one of [Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].\n"
                        + "    Instance[/tables/0/dimensions/0] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/0/cardinality] failed to validate against schema[/definitions/dimensionRef/properties/cardinality]. Cardinality type [Extra small] is not allowed. Supported value is one of [Tiny, Small, Medium, Large, Huge].\n"
                        + "        Instance[/tables/0/dimensions/0/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [id] is not allowed. Field name cannot be one of [id, sql]\n"
                        + "        Instance[/tables/0/dimensions/0/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [Float] is not allowed. Field type must be [Time] for any time dimension.\n"
                        + "    Instance[/tables/0/dimensions/0] failed to validate against schema[/definitions/timeDimension]. Properties: [tableSource] are not allowed for time dimensions.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/1] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 1 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/1/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [_region] is not allowed. Field name must start with lower case alphabet and can include alaphabets, numbers and '_' only.\n"
                        + "        Instance[/tables/0/dimensions/1/tags] failed to validate against schema[/definitions/dimensionRef/properties/tags]. instance type (string) does not match any allowed primitive type (allowed: [\"array\"])\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/dimension]. tableSource and values cannot both be defined for a dimension. Choose One or None.\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/1/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [_region] is not allowed. Field name must start with lower case alphabet and can include alaphabets, numbers and '_' only.\n"
                        + "        Instance[/tables/0/dimensions/1/tags] failed to validate against schema[/definitions/dimensionRef/properties/tags]. instance type (string) does not match any allowed primitive type (allowed: [\"array\"])\n"
                        + "        Instance[/tables/0/dimensions/1/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [Text] is not allowed. Field type must be [Time] for any time dimension.\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/timeDimension]. Properties: [values, tableSource] are not allowed for time dimensions.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/2] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/2] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 1 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/2/type] failed to validate against schema[/definitions/dimension/allOf/1/properties/type]. Field type [TIMEX] is not allowed. Supported value is one of [Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].\n"
                        + "    Instance[/tables/0/dimensions/2] failed to validate against schema[/definitions/dimension]. Properties: [grains] are not allowed for dimensions.\n"
                        + "    Instance[/tables/0/dimensions/2] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 1 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/2/grains/0/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/grains/items/properties/type]. Grain type [Days] is not allowed. Supported value is one of [Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year].\n"
                        + "        Instance[/tables/0/dimensions/2/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [TIMEX] is not allowed. Field type must be [Time] for any time dimension.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/filterTemplate] failed to validate against schema[/properties/tables/items/properties/filterTemplate]. Input value[countryIsoCode={{code}};startTime=={{start}}] is not a valid RSQL filter expression. Please visit page https://elide.io/pages/guide/v5/11-graphql.html#operators for samples.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/measures/0] failed to validate against schema[/definitions/measure]. instance failed to match exactly one schema (matched 2 out of 2)\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/measures/0/maker] failed to validate against schema[/definitions/measure/properties/maker]. Input value[com.yahoo.elide.datastores.aggregation.query@DefaultMetricProjectionMaker.class] is not a valid Java class name.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/name] failed to validate against schema[/properties/tables/items/properties/name]. Name [Country@10] is not allowed. Name must start with an alphabetic character and can include alaphabets, numbers and '_' only.";

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
                        + "Instance[/dbconfigs/0/driver] failed to validate against schema[/properties/dbconfigs/items/properties/driver]. Input value[11COM.ibm.db2.jdbc.net.DB2Driver] is not a valid Java class name.\n"
                        + "[ERROR]\n"
                        + "Instance[/dbconfigs/0/name] failed to validate against schema[/properties/dbconfigs/items/properties/name]. Name [11MyDB2Connection] is not allowed. Name must start with an alphabetic character and can include alaphabets, numbers and '_' only.\n"
                        + "[ERROR]\n"
                        + "Instance[/dbconfigs/0/propertyMap/hibernate.show_sql] failed to validate against schema[/properties/dbconfigs/items/properties/propertyMap/patternProperties/^([A-Za-z0-9_]+[.]?)+$]. instance type (null) does not match any allowed primitive type (allowed: [\"array\",\"boolean\",\"integer\",\"number\",\"object\",\"string\"])\n"
                        + "[ERROR]\n"
                        + "Instance[/dbconfigs/1/dialect] failed to validate against schema[/properties/dbconfigs/items/properties/dialect]. instance type (integer) does not match any allowed primitive type (allowed: [\"string\"])\n"
                        + "[ERROR]\n"
                        + "Instance[/dbconfigs/1/url] failed to validate against schema[/properties/dbconfigs/items/properties/url]. Input value [ojdbc:mysql://localhost/testdb?serverTimezone=UTC] is not a valid JDBC url, it must start with 'jdbc:'.";
        assertEquals(expectedMessage, e.getMessage());
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
