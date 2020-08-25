/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.datastores.aggregation.timegrains.DateTime;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeSerdeTest {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testDateSerialize() throws ParseException {

        String expected = "2020-01-01 01:00:00";
        DateTime expectedDate = new DateTime(formatter.parse(expected));
        DateTimeSerde dateSerde = new DateTimeSerde();
        Object actual = dateSerde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserializeString() throws ParseException {

        String dateInString = "2020-01-01 01:00:00";
        Date expectedDate = new Date(formatter.parse(dateInString).getTime());
        String actual = "2020-01-01 01:00:00";
        DateTimeSerde dateTimeSerde = new DateTimeSerde();
        Object actualDate = dateTimeSerde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {

        String dateInString = "2020-01-01 01:00:00";
        DateTime expectedDate = new DateTime(formatter.parse(dateInString));
        Timestamp timestamp = new java.sql.Timestamp(formatter.parse(dateInString).getTime());
        DateTimeSerde dateTimeSerde = new DateTimeSerde();
        Object actualDate = dateTimeSerde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "00:00:00 2020-01-01";
        DateTimeSerde dateTimeSerde = new DateTimeSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            dateTimeSerde.deserialize(dateInString);
        });
    }
}
