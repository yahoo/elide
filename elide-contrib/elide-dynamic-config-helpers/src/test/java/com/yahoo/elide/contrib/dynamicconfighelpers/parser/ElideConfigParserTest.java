/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Type;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

public class ElideConfigParserTest {

    private ElideConfigParser testClass = new ElideConfigParser();

    @Test
    public void testValidateVariablePath() throws Exception {

        String path = "src/test/resources/models";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        testClass.parseConfigPath(absolutePath);
        Map<String, Object> variable = testClass.getVariables();
        assertEquals(4, variable.size());
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
//            testClass.parseConfigPath(null);
        } catch (NullPointerException npe) {
            assertEquals("Config path is null", npe.getMessage());
        }
    }
}
