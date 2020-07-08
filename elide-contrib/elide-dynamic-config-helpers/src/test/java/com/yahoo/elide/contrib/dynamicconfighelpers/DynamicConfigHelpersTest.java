/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;

import org.junit.jupiter.api.Test;

public class DynamicConfigHelpersTest {

    @Test
    public void testValidSecuritySchema() throws IOException {
        String path = "src/test/resources/security/valid";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        Map<String, Object> vars =  DynamicConfigHelpers.getVariablesPojo(
                DynamicConfigHelpers.formatFilePath(absolutePath));
        ElideSecurityConfig config =  DynamicConfigHelpers.getElideSecurityPojo(
                DynamicConfigHelpers.formatFilePath(absolutePath), vars);
        assertNotNull(config);
    }

    @Test
    public void testValidVariableSchema() throws IOException {
        String path = "src/test/resources/variables/valid";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        Map<String, Object> config =  DynamicConfigHelpers.getVariablesPojo(
                DynamicConfigHelpers.formatFilePath(absolutePath));
        assertNotNull(config);
    }

    @Test
    public void testValidTableSchema() throws IOException {
        String path = "src/test/resources/tables";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        Map<String, Object> vars =  DynamicConfigHelpers.getVariablesPojo(
                DynamicConfigHelpers.formatFilePath(absolutePath));
        ElideTableConfig config =  DynamicConfigHelpers.getElideTablePojo(
                DynamicConfigHelpers.formatFilePath(absolutePath), vars, "valid/");
        assertNotNull(config);
    }
}
