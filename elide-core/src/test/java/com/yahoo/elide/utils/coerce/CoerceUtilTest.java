/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CoerceUtilTest {

    public enum Seasons { WINTER, SPRING }

    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestClass {
        public int field1;
        public int field2;
    }

    @Test
    public void testNoConversions() throws Exception {

        assertEquals(CoerceUtil.coerce(1, null), 1,
                "coerce returns value if target class null");

        assertEquals(CoerceUtil.coerce(null, Object.class), null,
                "coerce returns value if value is null");

        assertEquals(CoerceUtil.coerce(1, int.class), 1,
                "coerce returns value if value is assignable to target");
    }

    @Test
    public void testToEnumConversion() throws Exception {

        assertEquals(CoerceUtil.coerce(1, Seasons.class), Seasons.SPRING,
                "ToEnumConverter is called when target class is Enum");
    }

    @Test
    public void testBasicConversion() throws Exception {

        assertEquals(CoerceUtil.coerce(1, String.class), "1",
                "coerce converts int to String");

        assertEquals(CoerceUtil.coerce("1", Long.class), Long.valueOf("1"),
                "coerce converts String to Long");

        assertEquals(CoerceUtil.coerce(1.0, int.class), 1,
                "coerce converts float to int");
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testError() throws Exception {

        CoerceUtil.coerce('A', Seasons.class);
    }

    @Test
    public void testMapConversion() {

        //Input Map
        Map<String, Object> testMap = new LinkedHashMap<>();
        testMap.put("field1", 1);
        testMap.put("field2", 2);

        //ExpectedObject
        TestClass testClass = new TestClass(1, 2);

        assertEquals(CoerceUtil.coerce(testMap, TestClass.class), testClass,
                "FromMapConverter is called when source class is a Map");
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testMapError() throws Exception {

        //Input Map
        Map<String, Object> testMap = new LinkedHashMap<>();
        testMap.put("foo", "bar");
        testMap.put("baz", "qaz");

        CoerceUtil.coerce(testMap, TestClass.class);
    }

    @Test
    public void testDateConversion() throws Exception {

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"));
        cal.set(Calendar.YEAR, 2016);
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DAY_OF_MONTH, 4);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        assertTrue(CoerceUtil.coerce("2016-05-04T13:00:00-05", Date.class).equals(date),
                "coerce converts String to Date");
    }
}
