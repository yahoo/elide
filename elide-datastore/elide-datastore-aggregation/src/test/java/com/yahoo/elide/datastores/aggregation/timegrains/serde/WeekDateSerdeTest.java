/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.datastores.aggregation.timegrains.WeekDateISO;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WeekDateSerdeTest {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    public void testDateSerialize() throws ParseException {

        String expected = "2020-01-06";
        WeekDateISO expectedDate = new WeekDateISO(formatter.parse(expected));
        WeekDateSerde dateSerde = new WeekDateSerde();
        Object actual = dateSerde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateSerializeNotMonday() throws ParseException {

        String expected = "2020-01-01";
        WeekDateISO expectedDate = new WeekDateISO(formatter.parse(expected));
        WeekDateSerde dateSerde = new WeekDateSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            dateSerde.serialize(expectedDate);
        });
    }

    @Test
    public void testDateDeserializeString() throws ParseException {

        String dateInString = "2020-01-06";
        Date expectedDate = new Date(formatter.parse(dateInString).getTime());
        String actual = "2020-01-06";
        WeekDateSerde weekDateSerde = new WeekDateSerde();
        Object actualDate = weekDateSerde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestampNotMonday() throws ParseException {

        String dateInString = "2020-01-01";
        Timestamp timestamp = new Timestamp(formatter.parse(dateInString).getTime());
        WeekDateSerde weekDateSerde = new WeekDateSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            weekDateSerde.deserialize(timestamp);
        });
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {

        String dateInString = "2020-01-06";
        WeekDateISO expectedDate = new WeekDateISO(formatter.parse(dateInString));
        Timestamp timestamp = new Timestamp(formatter.parse(dateInString).getTime());
        WeekDateSerde weekDateSerde = new WeekDateSerde();
        Object actualDate = weekDateSerde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "January-2020-01";
        WeekDateSerde weekDateSerde = new WeekDateSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            weekDateSerde.deserialize(dateInString);
        });
    }
}
