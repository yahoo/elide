/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class DynamicConfigHelpersTest {

    @Test
    public void testValidSecuritySchema() throws IOException {
        String path = "src/test/resources/security/valid/security.hjson";
        File file = new File(path);
        ElideSecurityConfig config =  DynamicConfigHelpers.stringToElideSecurityPojo(
                DynamicConfigHelpers.readConfigFile(file), Collections.emptyMap());
        assertNotNull(config);
    }

    @Test
    public void testValidVariableSchema() throws IOException {
        String path = "src/test/resources/variables/valid/variables.hjson";
        File file = new File(path);
        String content = DynamicConfigHelpers.readConfigFile(file);
        Map<String, Object> config =  DynamicConfigHelpers.stringToVariablesPojo(content);
        assertNotNull(config);
    }

    @Test
    public void testValidTableSchema() throws IOException {
        String path = "src/test/resources/tables/valid/table.hjson";
        String tableContent = DynamicConfigHelpers.readConfigFile(new File(path));
        ElideTableConfig config =  DynamicConfigHelpers.stringToElideTablePojo(tableContent, Collections.emptyMap());
        assertNotNull(config);
    }
}
