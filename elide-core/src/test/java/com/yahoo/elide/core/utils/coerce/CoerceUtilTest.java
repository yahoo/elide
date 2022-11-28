/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.utils.coerce.converters.EpochToDateConverter;
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class CoerceUtilTest {

    public enum Seasons { WINTER, SPRING }
    public enum WeekendDays { SATURDAY, SUNDAY }

    private static Map<Class, Serde> oldSerdes = new HashMap<>();

    @BeforeAll
    public static void init() {
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

    @AfterAll
    public static void shutdown() {
        oldSerdes.forEach(CoerceUtil::register);
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

        assertEquals(1, CoerceUtil.coerce(1, (Class<Object>) null),
                     "coerce returns value if target class null");

        assertNull(CoerceUtil.coerce(null, Object.class),
                     "coerce returns value if value is null");

        assertEquals(1, (Object) CoerceUtil.coerce(1, int.class),
                "coerce returns value if value is assignable to target");
    }

    @Test
    public void testToEnumConversion() throws Exception {

        assertEquals(Seasons.SPRING, CoerceUtil.coerce(1, Seasons.class),
                "ToEnumConverter is called when target class is Enum");
    }

    @Test
    public void testToUUIDConversion() throws Exception {
        String uuidString = "11111111-2222-3333-4444-555555555555";

        assertEquals(
                UUID.fromString(uuidString),
                CoerceUtil.coerce(uuidString, UUID.class),
                "ToUUIDConverter is called when target class is UUID");
    }

    @Test
    public void testBasicConversion() throws Exception {

        assertEquals("1", CoerceUtil.coerce(1, String.class),
                "coerce converts int to String");

        assertEquals(Long.valueOf("1"), CoerceUtil.coerce("1", Long.class),
                "coerce converts String to Long");

        assertEquals(1, (Object) CoerceUtil.coerce(1.0, int.class),
                "coerce converts float to int");
    }

    @Test
    public void testError() throws Exception {

        assertThrows(InvalidValueException.class, () -> CoerceUtil.coerce('A', Seasons.class));
    }

    @Test
    public void testMapConversion() {

        //Input Map
        Map<String, Object> testMap = new LinkedHashMap<>();
        testMap.put("field1", 1);
        testMap.put("field2", 2);

        //ExpectedObject
        TestClass testClass = new TestClass(1, 2);

        assertEquals(testClass, CoerceUtil.coerce(testMap, TestClass.class),
                "FromMapConverter is called when source class is a Map");
    }

    @Test
    public void testMapError() throws Exception {

        //Input Map
        Map<String, Object> testMap = new LinkedHashMap<>();
        testMap.put("foo", "bar");
        testMap.put("baz", "qaz");

        assertThrows(InvalidValueException.class, () -> CoerceUtil.coerce(testMap, TestClass.class));
    }

    @Test
    public void testConversionFailure() throws Exception {
        assertThrows(InvalidValueException.class, () -> CoerceUtil.coerce("a", Long.class));
    }

    @Test
    public void testNullConversion() throws Exception {
        assertNull(CoerceUtil.coerce(null, String.class));
    }

    @Test
    public void testStringToDate() throws Exception {
        Date date = CoerceUtil.coerce("1", Date.class);
        assertEquals(new Date(1), date);

        java.sql.Date date1 = CoerceUtil.coerce("1", java.sql.Date.class);
        assertEquals(new java.sql.Date(1), date1);

        Timestamp timestamp = CoerceUtil.coerce("1", Timestamp.class);
        assertEquals(new Timestamp(1), timestamp);

        Time time = CoerceUtil.coerce("1", Time.class);
        assertEquals(new Time(1), time);
    }

    @Test
    public void testLongToDate() {
        Date dateLong = CoerceUtil.coerce(0L, Date.class);
        assertEquals(new Date(0), dateLong);

        java.sql.Date date1Long = CoerceUtil.coerce(0L, java.sql.Date.class);
        assertEquals(new java.sql.Date(0), date1Long);

        Timestamp timestampLong = CoerceUtil.coerce(0L, Timestamp.class);
        assertEquals(new Timestamp(0), timestampLong);

        Time timeLong = CoerceUtil.coerce(0L, Time.class);
        assertEquals(new Time(0), timeLong);
    }

    @Test
    public void testCustomEnumSerde() {
        Serde<String, WeekendDays> mockSerde = (Serde<String, WeekendDays>) mock(Serde.class);
        CoerceUtil.register(WeekendDays.class, mockSerde);

        CoerceUtil.coerce("Monday", WeekendDays.class);
        verify(mockSerde, times(1)).deserialize(eq("Monday"));
    }

    @Test
    public void testIntToDate() throws Exception {
        Date date = CoerceUtil.coerce(0, Date.class);
        assertEquals(new Date(0), date);

        java.sql.Date date1 = CoerceUtil.coerce(0, java.sql.Date.class);
        assertEquals(new java.sql.Date(0), date1);

        Timestamp timestamp = CoerceUtil.coerce(0, Timestamp.class);
        assertEquals(new Timestamp(0), timestamp);

        Time time = CoerceUtil.coerce(0, Time.class);
        assertEquals(new Time(0), time);
    }

    @Test
    public void testDateToTimestamp() {
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Serde oldDateSerde = CoerceUtil.lookup(Date.class);
        Serde oldTimestampSerde = CoerceUtil.lookup(java.sql.Timestamp.class);
        Serde timestampSerde = new ISO8601DateSerde(dateFormat, tz, java.sql.Timestamp.class);
        CoerceUtil.register(Date.class, new ISO8601DateSerde(dateFormat, tz));
        CoerceUtil.register(java.sql.Timestamp.class, timestampSerde);
        Date date = new Date();
        Timestamp timestamp = CoerceUtil.coerce(date, Timestamp.class);
        assertEquals(date.getTime(), timestamp.getTime());
        CoerceUtil.register(Date.class, oldDateSerde);
        CoerceUtil.register(java.sql.Timestamp.class, oldTimestampSerde);
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
        assertEquals(uuid, coercedFromPrimaryClassLoader);

        try {
            Thread.currentThread().setContextClassLoader(new SecondaryClassLoader());
            UUID coercedFromSecondaryClassLoader =  CoerceUtil.coerce(uuidString, UUID.class);
            assertEquals(uuid, coercedFromSecondaryClassLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    // Dummy class loader for test
    private static class SecondaryClassLoader extends ClassLoader { }
}
