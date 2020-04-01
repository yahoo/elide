/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Map;


public class ElideVariablesToPojoTest {

    private ElideConfigParser testClass = new ElideConfigParser();

    @Test
    public void testValidateVariable() throws Exception {
        String str = "{\n"
                       + "    fo:bar\n"
                       + "    fi:[1,2,3]\n"
                       + "    fum: this is a test!\n"
                       + "}";

        testClass.parseConfigString(str, "variable");
        Map<String, Object> variables = this.testClass.getVariables();

        assertEquals(3, variables.size());
        assertEquals("bar", variables.get("fo"));

        assertEquals("[1, 2, 3]", variables.get("fi").toString());
    }
}
