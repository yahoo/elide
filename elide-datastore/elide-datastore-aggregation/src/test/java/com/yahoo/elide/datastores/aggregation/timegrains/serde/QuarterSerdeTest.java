/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.datastores.aggregation.timegrains.Quarter;
import com.yahoo.elide.utils.coerce.converters.Serde;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class QuarterSerdeTest {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");

    @Test
    public void testDateSerialize() throws ParseException {

        String expected = "2020-01";
        Quarter expectedDate = new Quarter(formatter.parse(expected));
        Serde serde = new Quarter.QuarterSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserialize() throws ParseException {

        String dateInString = "2020-01";
        Quarter expectedDate = new Quarter(formatter.parse(dateInString));
        String actual = "2020-01";
        Serde serde = new Quarter.QuarterSerde();
        Object actualDate = serde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestampNotQuarterMonth() throws ParseException {

        String dateInString = "2020-02";
        Timestamp timestamp = new Timestamp(formatter.parse(dateInString).getTime());
        Serde serde = new Quarter.QuarterSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            serde.deserialize(timestamp);
        });
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {

        String dateInString = "2020-01";
        Quarter expectedDate = new Quarter(formatter.parse(dateInString));
        Timestamp timestamp = new Timestamp(formatter.parse(dateInString).getTime());
        Serde serde = new Quarter.QuarterSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "January-2020";
        Serde serde = new Quarter.QuarterSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            serde.deserialize(dateInString);
        });
    }
}
