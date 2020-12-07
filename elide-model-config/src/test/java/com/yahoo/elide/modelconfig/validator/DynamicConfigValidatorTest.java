/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.validator;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.modelconfig.model.Table;

import org.junit.jupiter.api.Test;

public class DynamicConfigValidatorTest {

    @Test
    public void testValidInheritanceConfig() throws Exception {
        DynamicConfigValidator testClass = new DynamicConfigValidator("src/test/resources/validator/valid");
        testClass.readAndValidateConfigs();
        Table parent = testClass.getElideTableConfig().getTable("PlayerStats");
        Table child = testClass.getElideTableConfig().getTable("PlayerStatsChild");

        // parent class dim + 3 new in child class + 2 overridden
        assertEquals(parent.getDimensions().size(), 4);
        assertEquals(child.getDimensions().size(), parent.getDimensions().size() + 3);

        // parent class measure + 1 new in child class
        assertEquals(parent.getMeasures().size(), 2);
        assertEquals(child.getMeasures().size(), parent.getMeasures().size() + 1);

        // parent class sql/table
        assertEquals("player_stats", child.getTable());
        assertEquals(null, child.getSql());
        assertEquals("gamedb", child.getSchema());
        assertEquals(null, child.getDbConnectionName());
        assertEquals(true, child.getIsFact());

        // no new joins in child class, will inherit parent class joins
        assertEquals(parent.getJoins().size(), child.getJoins().size());
    }

    @Test
    public void testHelpArgumnents() {
        assertDoesNotThrow(() -> DynamicConfigValidator.main(new String[] { "-h" }));
        assertDoesNotThrow(() -> DynamicConfigValidator.main(new String[] { "--help" }));
    }

    @Test
    public void testNoArgumnents() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator.main(null));
            assertEquals(1, exitStatus);
        });

        assertTrue(error.startsWith("Missing required option"));
    }

    @Test
    public void testOneEmptyArgument() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator.main(new String[] { "" }));
            assertEquals(1, exitStatus);
        });

        assertTrue(error.startsWith("Missing required option"));
    }

    @Test
    public void testMissingArgumentValue() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator.main(new String[] { "--configDir" }));
            assertEquals(2, exitStatus);
        });

        assertTrue(error.startsWith("Missing argument for option"));

        error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator.main(new String[] { "-c" }));
            assertEquals(2, exitStatus);
        });

        assertTrue(error.startsWith("Missing argument for option"));
    }

    @Test
    public void testMissingConfigDir() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/missing" }));
            assertEquals(2, exitStatus);
        });

        assertTrue(error.contains("config path does not exist"));
    }

    @Test
    public void testMissingVariableConfig() {
        assertDoesNotThrow(() -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/missing_variable" }));
    }

    @Test
    public void testMissingSecurityConfig() {
        assertDoesNotThrow(() -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/missing_security" }));
    }

    @Test
    public void testMissingConfigs() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/missing_configs" }));
            assertEquals(2, exitStatus);
        });

        assertTrue(error.startsWith("Neither Table nor DB configs found under:"));
    }

    @Test
    public void testMissingTableConfig() {
        assertDoesNotThrow(() -> DynamicConfigValidator
                        .main(new String[] { "--configDir", "src/test/resources/validator/missing_table_config" }));
    }

    @Test
    public void testBadVariableConfig() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_variable" }));
            assertEquals(2, exitStatus);
        });

        assertEquals("Invalid Hjson Syntax: Found '[' where a key name was "
                + "expected (check your syntax or use quotes if the key name includes {}[],: or whitespace) at 3:7\n",
                error);
    }

    @Test
    public void testInheritanceCycle() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_cyclic_inheritance" }));
            assertEquals(2, exitStatus);
        });

        assertTrue(error.contains("Inheriting from table"));
        assertTrue(error.contains("creates an illegal cyclic dependency."));
    }

    @Test
    public void testMissingInheritanceModel() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/missing_inheritance" }));
            assertEquals(2, exitStatus);
        });

        assertEquals("Undefined model: B is used as a Parent(extend) for another model.\n", error);
    }

    @Test
    public void testBadSecurityConfig() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_security" }));
            assertEquals(2, exitStatus);
        });

        assertEquals("Invalid Hjson Syntax: Found '[' where a key name was expected "
                + "(check your syntax or use quotes if the key name includes {}[],: or whitespace) at 3:11\n",
                error);
    }

    @Test
    public void testBadSecurityRoleConfig() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_security_role" }));
            assertEquals(2, exitStatus);
        });

        assertEquals("ROLE provided in security config contain one of these words: [,]\n", error);
    }

    @Test
    public void testBadTableConfigJoinType() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_table_join_type" }));
            assertEquals(2, exitStatus);
        });
        String expected = "Schema validation failed for: table1.hjson\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/joins/0/kind] failed to validate against schema[/definitions/join/properties/kind]. Join kind [toAll] is not allowed. Supported value is one of [ToOne, ToMany].\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/joins/1/type] failed to validate against schema[/definitions/join/properties/type]. Join type [full outer] is not allowed. Supported value is one of [left, inner, full, cross].\n";

        assertEquals(expected, error);
    }

    @Test
    public void testBadDimName() throws Exception {
        String expectedMessage = "Schema validation failed for: table1.hjson\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/0] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/0] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 1 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/0/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [id] is not allowed. Field name cannot be 'id'\n"
                        + "    Instance[/tables/0/dimensions/0] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/0/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [id] is not allowed. Field name cannot be 'id'\n"
                        + "        Instance[/tables/0/dimensions/0/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [Text] is not allowed. Field type must be [Time] for any time dimension.\n"
                        + "[ERROR]\n"
                        + "Instance[/tables/0/dimensions/1] failed to validate against schema[/properties/tables/items/properties/dimensions/items]. instance failed to match exactly one schema (matched 0 out of 2)\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/dimension]. instance failed to match all required schemas (matched only 1 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/1/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [_region] is not allowed. Field name must start with an alphabet and can include alaphabets, numbers and '_' only.\n"
                        + "    Instance[/tables/0/dimensions/1] failed to validate against schema[/definitions/timeDimension]. instance failed to match all required schemas (matched only 0 out of 2)\n"
                        + "        Instance[/tables/0/dimensions/1/name] failed to validate against schema[/definitions/dimensionRef/properties/name]. Field name [_region] is not allowed. Field name must start with an alphabet and can include alaphabets, numbers and '_' only.\n"
                        + "        Instance[/tables/0/dimensions/1/type] failed to validate against schema[/definitions/timeDimension/allOf/1/properties/type]. Field type [Text] is not allowed. Field type must be [Time] for any time dimension.\n";

        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_dim_name" }));
            assertEquals(2, exitStatus);
        });

        assertEquals(expectedMessage, error);
    }

    @Test
    public void testBadTableConfigSQL() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_table_sql" }));
            assertEquals(2, exitStatus);
        });

        assertTrue(error.startsWith("sql/definition provided in table config contain either ';' or one of these words"));
    }

    @Test
    public void testBadJoinModel() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() ->
                    DynamicConfigValidator.main(new String[] { "--configDir", "src/test/resources/validator/bad_join_model" }));
            assertEquals(2, exitStatus);
        });
        assertTrue(error.contains(" is neither included in dynamic models nor in static models"));
    }

    @Test
    public void testBadJoinDefinition() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator
                    .main(new String[]{"--configDir", "src/test/resources/validator/bad_join_def"}));

            assertEquals(2, exitStatus);
        });

        assertTrue(error.startsWith("Join name must be used before '.' in join definition."));
    }

    @Test
    public void testUndefinedVariable() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator
                    .main(new String[]{"--configDir", "src/test/resources/validator/undefined_handlebar"}));

            assertEquals(2, exitStatus);
        });

        assertEquals("foobar is used as a variable in either table or security config files "
                        + "but is not defined in variables config file.\n", error);
    }

    public void testBadTableSource() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator
                    .main(new String[]{"--configDir", "src/test/resources/validator/bad_tablesource"}));

            assertEquals(2, exitStatus);
        });
        assertEquals("Invalid tableSource : Team.teamRegion . Field : teamRegion is undefined for hjson model: Team",
                        error);
    }

    @Test
    public void testDuplicateDBConfigName() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator
                    .main(new String[]{"--configDir", "src/test/resources/validator/duplicate_dbconfigname"}));

            assertEquals(2, exitStatus);
        });

        assertEquals("Duplicate!! Either Table or DB configs found with the same name.\n", error);
    }

    @Test
    public void testJoinedTablesDBConnectionNameMismatch() throws Exception {
        String error = tapSystemErr(() -> {
            int exitStatus = catchSystemExit(() -> DynamicConfigValidator
                    .main(new String[]{"--configDir", "src/test/resources/validator/mismatch_dbconfig"}));

            assertEquals(2, exitStatus);
        });
        assertTrue(error.contains("DBConnection name mismatch between table: "));
        assertTrue(error.contains(" and tables in its Join Clause."));
    }

    @Test
    public void testFormatClassPath() {
        assertEquals("anydir", DynamicConfigValidator.formatClassPath("src/test/resources/anydir"));
        assertEquals("anydir/configs", DynamicConfigValidator.formatClassPath("src/test/resources/anydir/configs"));
        assertEquals("src/test/resourc", DynamicConfigValidator.formatClassPath("src/test/resourc"));
        assertEquals("", DynamicConfigValidator.formatClassPath("src/test/resources/"));
        assertEquals("", DynamicConfigValidator.formatClassPath("src/test/resources"));
        assertEquals("anydir/configs", DynamicConfigValidator.formatClassPath("src/test/resourcesanydir/configs"));
    }
}
