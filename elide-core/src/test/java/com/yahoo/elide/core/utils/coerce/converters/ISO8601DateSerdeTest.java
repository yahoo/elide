/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.TimeZone;

public class ISO8601DateSerdeTest {
    @Test
    public void testDateSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        assertEquals("1970-01-01T00:00Z", serde.serialize(new Date(0)));
    }

    @Test
    public void testDateDeserialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        assertEquals(serde.deserialize("1970-01-01T00:00Z"), new Date(0));
    }

    @Test
    public void testSQLDateSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        assertEquals("1970-01-01T00:00Z", serde.serialize(new java.sql.Date(0)));
    }

    @Test
    public void testSQLDateDeserialization() throws Exception {
        ISO8601DateSerde serde =
            new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"), java.sql.Date.class);
        assertEquals(serde.deserialize("1970-01-01T00:00Z"), new java.sql.Date(0));
    }

    @Test
    public void testSQLTimeSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        assertEquals("1970-01-01T00:00Z", serde.serialize(new java.sql.Time(0)));
    }

    @Test
    public void testSQLTimeDeserialization() throws Exception {
        ISO8601DateSerde serde =
            new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"), java.sql.Time.class);
        assertEquals(serde.deserialize("1970-01-01T00:00Z"), new java.sql.Time(0));
    }

    @Test
    public void testSQLTimestampSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        assertEquals("1970-01-01T00:00Z", serde.serialize(new java.sql.Timestamp(0)));
    }

    @Test
    public void testSQLTimestampDeserialization() throws Exception {
        ISO8601DateSerde serde =
                new ISO8601DateSerde("yyyy-MM-dd", TimeZone.getTimeZone("UTC"), java.sql.Timestamp.class);
        java.sql.Timestamp expected = new java.sql.Timestamp(0);
        java.sql.Timestamp actual = (java.sql.Timestamp) serde.deserialize("1970-01-01");
        assertEquals(expected, actual);
    }

    @Test
    public void testDateToSQLTimestampDeserialization() throws Exception {
        ISO8601DateSerde serde =
                new ISO8601DateSerde("yyyy-MM-dd", TimeZone.getTimeZone("UTC"), java.sql.Timestamp.class);
        Timestamp expected = new Timestamp(0);
        Timestamp actual = (Timestamp) serde.deserialize(new Date(0));
        assertEquals(expected, actual);
    }

    @Test
    public void testInvalidDateDeserialization() {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        assertThrows(IllegalArgumentException.class, () -> serde.deserialize("1"));
    }

    @Test
    public void testInvalidDateDeserializationMessage() {
        ISO8601DateSerde serde = new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> serde.deserialize("2019-01-01T00:00Z"));
        assertEquals("Date strings must be formatted as yyyy-MM-dd'T'HH:mm:ss'Z'", e.getMessage());
    }
}
