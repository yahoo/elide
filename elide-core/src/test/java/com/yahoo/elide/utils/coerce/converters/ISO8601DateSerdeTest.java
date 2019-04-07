/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.TimeZone;

public class ISO8601DateSerdeTest {
    @Test
    public void testDateSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        Assert.assertEquals(serde.serialize(new Date(0)), "1970-01-01T00:00Z");
    }

    @Test
    public void testDateDeserialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        Assert.assertEquals(new Date(0), serde.deserialize("1970-01-01T00:00Z"));
    }

    @Test
    public void testSQLDateSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        Assert.assertEquals(serde.serialize(new java.sql.Date(0)), "1970-01-01T00:00Z");
    }

    @Test
    public void testSQLDateDeserialization() throws Exception {
        ISO8601DateSerde serde =
            new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"), java.sql.Date.class);
        Assert.assertEquals(new java.sql.Date(0), serde.deserialize("1970-01-01T00:00Z"));
    }

    @Test
    public void testSQLTimeSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        Assert.assertEquals(serde.serialize(new java.sql.Time(0)), "1970-01-01T00:00Z");
    }

    @Test
    public void testSQLTimeDeserialization() throws Exception {
        ISO8601DateSerde serde =
            new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"), java.sql.Time.class);
        Assert.assertEquals(new java.sql.Time(0), serde.deserialize("1970-01-01T00:00Z"));
    }

    @Test
    public void testSQLTimestampSerialization() throws Exception {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        Assert.assertEquals(serde.serialize(new java.sql.Timestamp(0)), "1970-01-01T00:00Z");
    }

    @Test
    public void testSQLTimestampDeserialization() throws Exception {
        ISO8601DateSerde serde =
                new ISO8601DateSerde("yyyy-MM-dd", TimeZone.getTimeZone("UTC"), java.sql.Timestamp.class);
        java.sql.Timestamp expected = new java.sql.Timestamp(0);
        java.sql.Timestamp actual = (java.sql.Timestamp) serde.deserialize("1970-01-01");
        Assert.assertEquals(actual, expected);
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidDateDeserialization() {
        ISO8601DateSerde serde = new ISO8601DateSerde();
        serde.deserialize("1");
    }

    @Test
    public void testInvalidDateDeserializationMessage() {
        ISO8601DateSerde serde = new ISO8601DateSerde("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"));
        try {
            serde.deserialize("2019-01-01T00:00Z");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "Date strings must be formated as yyyy-MM-dd'T'HH:mm:ss'Z'");
        }
    }
}
