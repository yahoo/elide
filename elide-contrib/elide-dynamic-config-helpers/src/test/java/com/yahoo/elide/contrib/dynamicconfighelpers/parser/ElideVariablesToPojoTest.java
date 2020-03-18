/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * ElideVariablesToPojo test.
 */
public class ElideVariablesToPojoTest {

    private ElideVariableToPojo testClass = new ElideVariableToPojo();

    @Test
    public void testValidateVariable() throws Exception {
        String str = "{\n"
                       + "    fo:bar\n"
                       + "    fi:[1,2,3]\n"
                       + "    fum: this is a test!\n"
                       + "}";
        Map<String, Object> map = testClass.parseVariableConfig(str);

        assertEquals(3, map.size());
        assertEquals("bar", map.get("fo"));

        assertEquals("[1, 2, 3]", map.get("fi").toString());
    }

    @Test
    public void testValidateVariableInvalid() throws Exception {
        assertNull(testClass.parseVariableConfig(""));
    }

    @Test
    public void testValidateFilePath() {
        String filePath = "https://raw.githubusercontent.com/hjson/hjson/master/testCases/trail_result.hjson";

        Map<String, Object> map = testClass.parseVariableConfigFile(filePath);

        assertEquals(1, map.size());
        assertEquals("0   -- this string starts at 0 and ends at 1, preceding and trailing whitespace is ignored --   1", map.get("foo"));
    }
}
