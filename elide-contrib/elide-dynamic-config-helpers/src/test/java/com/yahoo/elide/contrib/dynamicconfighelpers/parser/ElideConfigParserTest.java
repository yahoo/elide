/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Type;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

public class ElideConfigParserTest {

    @BeforeAll
    public static void setup() {
        DynamicConfigHelpers.setTableConfigPath("tables/");
    }

    @Test
    public void testValidateVariablePath() throws Exception {

        String path = "src/test/resources/models";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        ElideConfigParser testClass = new ElideConfigParser(absolutePath);

        Map<String, Object> variable = testClass.getVariables();
        assertEquals(6, variable.size());
        assertEquals("blah", variable.get("bar"));

        ElideSecurityConfig security = testClass.getElideSecurityConfig();
        assertEquals(3, security.getRoles().size());

        ElideTableConfig tables = testClass.getElideTableConfig();
        assertEquals(2, tables.getTables().size());
        for (Table t : tables.getTables()) {
            assertEquals(t.getMeasures().get(0).getName() , t.getMeasures().get(0).getDescription());
            assertEquals("MAX(score)", t.getMeasures().get(0).getDefinition());
            assertEquals(Table.Cardinality.LARGE, t.getCardinality());
            // test hydration, variable substitution
            assertEquals(Type.INTEGER, t.getMeasures().get(0).getType());
        }
    }

    @Test
    public void testNullConfig() {
        try {
            new ElideConfigParser(null);
        } catch (IllegalArgumentException e) {
            assertEquals("Config path is null", e.getMessage());
        }
    }

    @Test
    public void testMissingConfig() {
        String path = "src/test/resources/models_missing";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        ElideConfigParser testClass = new ElideConfigParser(absolutePath);

        assertNull(testClass.getVariables());
        assertNull(testClass.getElideSecurityConfig());
    }
}
