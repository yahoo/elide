/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.utils.coerce.converters.EpochToDateConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CoerceUtilTest {

    public enum Seasons { WINTER, SPRING }

    private Map<Class, Serde> oldSerdes = new HashMap<>();

    @BeforeTest
    public void init() {
        Class [] dateClasses = {
                Date.class,
                java.sql.Date.class,
                Timestamp.class,
                Time.class
        };

        for (Class dateClass : dateClasses) {
            oldSerdes.put(dateClass, CoerceUtil.lookup(dateClass));
            CoerceUtil.register(dateClass, new EpochToDateConverter(dateClass));
        }
    }

    @AfterTest
    public void shutdown() {
        oldSerdes.forEach((dateClass, serde) -> {
            CoerceUtil.register(dateClass, serde);

        });
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestClass {
        public int field1;
        public int field2;
    }

    @Test
    public void testNoConversions() throws Exception {

        assertEquals(CoerceUtil.coerce(1, (Class<Object>) null), 1,
                     "coerce returns value if target class null");

        assertEquals(CoerceUtil.coerce(null, Object.class), null,
                     "coerce returns value if value is null");

        assertEquals((Object) CoerceUtil.coerce(1, int.class), 1,
                "coerce returns value if value is assignable to target");
    }

    @Test
    public void testToEnumConversion() throws Exception {

        assertEquals(CoerceUtil.coerce(1, Seasons.class), Seasons.SPRING,
                "ToEnumConverter is called when target class is Enum");
    }

    @Test
    public void testToUUIDConversion() throws Exception {
        String uuidString = "11111111-2222-3333-4444-555555555555";

        assertEquals(CoerceUtil.coerce(uuidString, UUID.class),
                         UUID.fromString(uuidString),
                "ToUUIDConverter is called when target class is UUID");
    }

    @Test
    public void testBasicConversion() throws Exception {

        assertEquals(CoerceUtil.coerce(1, String.class), "1",
                "coerce converts int to String");

        assertEquals(CoerceUtil.coerce("1", Long.class), Long.valueOf("1"),
                "coerce converts String to Long");

        assertEquals((Object) CoerceUtil.coerce(1.0, int.class), 1,
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

    @Test(expectedExceptions = InvalidValueException.class)
    public void testConversionFailure() throws Exception {
        CoerceUtil.coerce("a", Long.class);
    }

    @Test
    public void testNullConversion() throws Exception {
        assertNull(CoerceUtil.coerce(null, String.class));
    }

    @Test
    public void testStringToDate() throws Exception {
        Date date = CoerceUtil.coerce("1", Date.class);
        assertEquals(date, new Date(1));

        java.sql.Date date1 = CoerceUtil.coerce("1", java.sql.Date.class);
        assertEquals(date1, new java.sql.Date(1));

        Timestamp timestamp = CoerceUtil.coerce("1", Timestamp.class);
        assertEquals(timestamp, new Timestamp(1));

        Time time = CoerceUtil.coerce("1", Time.class);
        assertEquals(time, new Time(1));
    }

    @Test
    public void testLongToDate() {
        Date dateLong = CoerceUtil.coerce(0L, Date.class);
        assertEquals(dateLong, new Date(0));

        java.sql.Date date1Long = CoerceUtil.coerce(0L, java.sql.Date.class);
        assertEquals(date1Long, new java.sql.Date(0));

        Timestamp timestampLong = CoerceUtil.coerce(0L, Timestamp.class);
        assertEquals(timestampLong, new Timestamp(0));

        Time timeLong = CoerceUtil.coerce(0L, Time.class);
        assertEquals(timeLong, new Time(0));
    }

    @Test
    public void testIntToDate() throws Exception {
        Date date = CoerceUtil.coerce(0, Date.class);
        assertEquals(date, new Date(0));

        java.sql.Date date1 = CoerceUtil.coerce(0, java.sql.Date.class);
        assertEquals(date1, new java.sql.Date(0));

        Timestamp timestamp = CoerceUtil.coerce(0, Timestamp.class);
        assertEquals(timestamp, new Timestamp(0));

        Time time = CoerceUtil.coerce(0, Time.class);
        assertEquals(time, new Time(0));
    }

    /**
     * NOTE: BeanUtilsBean is documented as a <em>pseudo-singleton</em>.
     * https://commons.apache.org/proper/commons-beanutils/javadocs/v1.9.2/apidocs/org/apache/commons/beanutils/BeanUtilsBean.html
     */
    @Test
    public void testMultipleClassLoaders() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        UUID coercedFromPrimaryClassLoader = CoerceUtil.coerce(uuidString, UUID.class);
        assertEquals(coercedFromPrimaryClassLoader, uuid);

        try {
            Thread.currentThread().setContextClassLoader(new SecondaryClassLoader());
            UUID coercedFromSecondaryClassLoader =  CoerceUtil.coerce(uuidString, UUID.class);
            assertEquals(coercedFromSecondaryClassLoader, uuid);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    // Dummy class loader for test
    private static class SecondaryClassLoader extends ClassLoader { }
}
