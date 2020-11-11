/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.Dimension;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Measure;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DynamicConfigValidatorTest {

    @Test
    public void testHelpArgumnents() {
        assertDoesNotThrow(() -> DynamicConfigValidator.main(new String[] { "-h" }));
        assertDoesNotThrow(() -> DynamicConfigValidator.main(new String[] { "--help" }));
    }

    @Test
    public void testNoArgumnents() {
        Exception e = assertThrows(MissingOptionException.class, () -> DynamicConfigValidator.main(null));
        assertTrue(e.getMessage().startsWith("Missing required option"));
    }

    @Test
    public void testOneEmptyArgumnents() {
        Exception e = assertThrows(MissingOptionException.class,
                () -> DynamicConfigValidator.main(new String[] { "" }));
        assertTrue(e.getMessage().startsWith("Missing required option"));
    }

    @Test
    public void testMissingArgumnentValue() {
        Exception e = assertThrows(MissingArgumentException.class,
                () -> DynamicConfigValidator.main(new String[] { "--configDir" }));
        assertTrue(e.getMessage().startsWith("Missing argument for option"));
        e = assertThrows(MissingArgumentException.class, () -> DynamicConfigValidator.main(new String[] { "-c" }));
        assertTrue(e.getMessage().startsWith("Missing argument for option"));
    }

    @Test
    public void testMissingConfigDir() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/missing" }));
        assertTrue(e.getMessage().contains("config path does not exist"));
    }

    @Test
    public void testValidConfigDir() throws IOException {
        DynamicConfigValidator validator = new DynamicConfigValidator("src/test/resources/validator/valid");
        validator.readAndValidateConfigs();
        for (Table t : validator.getElideTableConfig().getTables()) {
            if (t.getName().equals("PlayerStatsChild")) {
                // test override flag for measures
                for (Measure m : t.getMeasures()) {
                    if (m.getName().equals("highScore")) {
                        assertTrue(m.isOverride());
                    }
                    else if (m.getName().equals("AvgScore")) {
                        assertFalse(m.isOverride());
                    }
                }
                // test override flag for dimensions
                for (Dimension dim : t.getDimensions()) {
                    if (dim.getName().equals("createdOn")) {
                        assertTrue(dim.isOverride());
                    }
                    else if (dim.getName().equals("updatedMonth")) {
                        assertFalse(dim.isOverride());
                    }
                }
                break;
            }
        }
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
    public void testMissingConfigs() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/missing_configs" }));
        assertTrue(e.getMessage().startsWith("Neither Table nor DB configs found under:"));
    }

    @Test
    public void testMissingTableConfig() {
        assertDoesNotThrow(() -> DynamicConfigValidator
                        .main(new String[] { "--configDir", "src/test/resources/validator/missing_table_config" }));
    }

    @Test
    public void testBadVariableConfig() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_variable" }));

        assertEquals("Invalid Hjson Syntax: Found '[' where a key name was "
                + "expected (check your syntax or use quotes if the key name includes {}[],: or whitespace) at 3:7",
                e.getMessage());
    }

    @Test
    public void testBadSecurityConfig() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_security" }));

        assertEquals("Invalid Hjson Syntax: Found '[' where a key name was expected "
                + "(check your syntax or use quotes if the key name includes {}[],: or whitespace) at 3:11",
                e.getMessage());
    }

    @Test
    public void testBadSecurityRoleConfig() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_security_role" }));
        assertEquals("ROLE provided in security config contain one of these words: [,]", e.getMessage());
    }

    @Test
    public void testBadTableConfigJoinType() {
        String expectedMessage = "Schema validation failed for: table1.hjson\n"
                        + "instance failed to match at least one required schema among 2 at node: /tables/0/joins/0/type\n"
                        + "    ECMA 262 regex \"^[Tt][Oo][Oo][Nn][Ee]$\" does not match input string \"toAll\" at node: /tables/0/joins/0/type\n"
                        + "    ECMA 262 regex \"^[Tt][Oo][Mm][Aa][Nn][Yy]$\" does not match input string \"toAll\" at node: /tables/0/joins/0/type";

        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                        .main(new String[] {"--configDir", "src/test/resources/validator/bad_table_join_type"}));
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testBadDimName() {
        String expectedMessage = "Schema validation failed for: table1.hjson\n"
                        + "instance failed to match at least one required schema among 2 at node: /tables/0/dimensions/1\n"
                        + "    instance failed to match all required schemas (matched only 1 out of 2) at node: /tables/0/dimensions/1\n"
                        + "        ECMA 262 regex \"^[A-Za-z]([0-9A-Za-z]*_?[0-9A-Za-z]*)*$\" does not match input string \"_region\" at node: /tables/0/dimensions/1/name\n"
                        + "    instance failed to match all required schemas (matched only 0 out of 2) at node: /tables/0/dimensions/1\n"
                        + "        ECMA 262 regex \"^[A-Za-z]([0-9A-Za-z]*_?[0-9A-Za-z]*)*$\" does not match input string \"_region\" at node: /tables/0/dimensions/1/name\n"
                        + "        ECMA 262 regex \"^[Tt][Ii][Mm][Ee]$\" does not match input string \"Text\" at node: /tables/0/dimensions/1/type";

        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                        .main(new String[] {"--configDir", "src/test/resources/validator/bad_dim_name"}));
        assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testBadTableConfigSQL() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_table_sql" }));
        assertTrue(e.getMessage()
                .startsWith("sql/definition provided in table config contain either ';' or one of these words"));
    }

    @Test
    public void testBadJoinModel() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                        .main(new String[] {"--configDir", "src/test/resources/validator/bad_join_model"}));
        assertTrue(e.getMessage().contains(" is neither included in dynamic models nor in static models"));
    }

    @Test
    public void testBadJoinDefinition() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                        .main(new String[] {"--configDir", "src/test/resources/validator/bad_join_def"}));
        assertTrue(e.getMessage().startsWith("Join name must be used before '.' in join definition."));
    }

    @Test
    public void testUndefinedVariable() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/undefined_handlebar" }));
        assertEquals("foobar is used as a variable in either table or security config files "
                        + "but is not defined in variables config file.", e.getMessage());
    }

    @Test
    public void testDuplicateDBConfigName() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/duplicate_dbconfigname" }));
        assertEquals("Duplicate!! Either Table or DB configs found with the same name.", e.getMessage());
    }

    @Test
    public void testJoinedTablesDBConnectionNameMismatch() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/mismatch_dbconfig" }));
        assertTrue(e.getMessage().contains("DBConnection name mismatch between table: "));
        assertTrue(e.getMessage().contains(" and tables in its Join Clause."));
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
