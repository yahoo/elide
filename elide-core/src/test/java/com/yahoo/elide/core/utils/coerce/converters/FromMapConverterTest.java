/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.beanutils.Converter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

public class FromMapConverterTest {

    private Converter converter;

    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestClass {
        public String field1;
        public String field2;
        public TestClass nestedField;
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.converter = new FromMapConverter();
    }

    @Test
    public void testMapToClass() throws Exception {

        //Input Map
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        nestedMap.put("field1", "value3");
        nestedMap.put("field2", "value4");

        Map<String, Object> testMap = new LinkedHashMap<>();
        testMap.put("field1", "value1");
        testMap.put("field2", "value2");
        testMap.put("nestedField", nestedMap);

        //ExpectedObject
        TestClass nestedClass = new TestClass("value3", "value4", null);
        TestClass testClass = new TestClass("value1", "value2", nestedClass);

        assertEquals(testClass, converter.convert(TestClass.class, testMap),
                "Map converter correctly converted Map to TestClass");
    }

    @Test
    public void testMapToMap() throws Exception {

        Map<String, Object> nestedMap = new LinkedHashMap<>();
        nestedMap.put("field1", "value3");
        nestedMap.put("field2", "value4");

        Map<String, Object> testMap = new LinkedHashMap<>();
        testMap.put("field1", "value1");
        testMap.put("field2", "value2");
        testMap.put("nestedField", nestedMap);

        assertEquals(testMap, converter.convert(Map.class, testMap),
                "Map converter correctly converted Map to Map");
    }
}
