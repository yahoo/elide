/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

<<<<<<< HEAD
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

=======
>>>>>>> elide-5.x
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.hjson.ParseException;
import org.junit.jupiter.api.Test;

<<<<<<< HEAD
=======
import java.io.IOException;

>>>>>>> elide-5.x
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
<<<<<<< HEAD
        assertEquals("Model Configs Directory doesn't exists", e.getMessage());
=======
        assertTrue(e.getMessage().startsWith("No Table configs found at"));
>>>>>>> elide-5.x
    }

    @Test
    public void testValidConfigDir() {
        assertDoesNotThrow(() -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/valid" }));
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
    public void testMissingTableDir() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/missing_table_dir" }));
<<<<<<< HEAD
        assertTrue(e.getMessage().startsWith("Table Configs Directory doesn't exists at location"));
=======
        assertTrue(e.getMessage().startsWith("No Table configs found at"));
>>>>>>> elide-5.x
    }

    @Test
    public void testMissingTableConfig() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/missing_table" }));
<<<<<<< HEAD
        assertTrue(e.getMessage().startsWith("No Table Configs found at location"));
=======
        assertTrue(e.getMessage().startsWith("No Table configs found at"));
>>>>>>> elide-5.x
    }

    @Test
    public void testBadVariableConfig() {
        assertThrows(ParseException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_variable" }));
    }

    @Test
    public void testBadSecurityConfig() {
        assertThrows(ParseException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_security" }));
    }

    @Test
    public void testBadSecurityRoleConfig() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_security_role" }));
        assertEquals(e.getMessage(), "ROLE provided in security config contain one of these words: [,]");
    }

    @Test
    public void testBadTableConfigJoinType() {
<<<<<<< HEAD
        assertThrows(ValueInstantiationException.class, () -> DynamicConfigValidator
=======
        assertThrows(IOException.class, () -> DynamicConfigValidator
>>>>>>> elide-5.x
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_table_join_type" }));
    }

    @Test
    public void testBadTableConfigSQL() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/bad_table_sql" }));
        assertTrue(e.getMessage()
                .startsWith("sql/definition provided in table config contain either ';' or one of these words"));
    }

    @Test
    public void testUndefinedVariable() {
        Exception e = assertThrows(IllegalStateException.class, () -> DynamicConfigValidator
                .main(new String[] { "--configDir", "src/test/resources/validator/undefined_handlebar" }));
        assertEquals(e.getMessage(),
                "foobar is used as a variable in either table or security config files but is not defined in variables config file.");
    }
}
