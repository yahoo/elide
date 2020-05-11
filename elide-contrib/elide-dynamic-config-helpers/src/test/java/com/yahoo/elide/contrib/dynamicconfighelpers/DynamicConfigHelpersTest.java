/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;

@Slf4j
public class DynamicConfigHelpersTest {

    @BeforeAll
    public static void setup() {
        DynamicConfigHelpers.setTableConfigPath("tables/");
    }

    @Test
    public void testValidSecuritySchema() {
        String path = "src/test/resources/security/valid";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        try {
            ElideSecurityConfig config =  DynamicConfigHelpers.getElideSecurityPojo(
                    DynamicConfigHelpers.formatFilePath(absolutePath));
            assertNotNull(config);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void testValidVariableSchema() {
        String path = "src/test/resources/variables/valid";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        try {
            Map<String, Object> config =  DynamicConfigHelpers.getVariablesPojo(
                    DynamicConfigHelpers.formatFilePath(absolutePath));
            assertNotNull(config);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    @Test
    public void testValidTableSchema() {
        DynamicConfigHelpers.setTableConfigPath("valid/");
        String path = "src/test/resources/tables";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        try {
            ElideTableConfig config =  DynamicConfigHelpers.getElideTablePojo(
                    DynamicConfigHelpers.formatFilePath(absolutePath));
            assertNotNull(config);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
