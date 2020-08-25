/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.datastores.aggregation.timegrains.MonthYear;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MonthYearSerdeTest {
    private SimpleDateFormat formatter = new SimpleDateFormat("MMM yyyy");

    @Test
    public void testDateSerialize() throws ParseException {

        String expected = "Jan 2020";
        MonthYear expectedDate = new  MonthYear(formatter.parse(expected));
        MonthYearSerde dateSerde = new MonthYearSerde();
        Object actual = dateSerde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserialize() throws ParseException {

        String dateInString = "Jan 2020";
        MonthYear expectedDate = new  MonthYear(formatter.parse(dateInString));
        String actual = "Jan 2020";
        MonthYearSerde dateSerde = new MonthYearSerde();
        Object actualDate = dateSerde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {

        String dateInString = "Jan 2020";
        MonthYear expectedDate = new MonthYear(formatter.parse(dateInString));
        Timestamp timestamp = new java.sql.Timestamp(formatter.parse(dateInString).getTime());
        //timestamp.toString() = 2020-01-01 00:00:00.0
        MonthYearSerde dateSerde = new MonthYearSerde();
        Object actualDate = dateSerde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "2020 January";
        MonthYearSerde dateSerde = new MonthYearSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            dateSerde.deserialize(dateInString);
        });
    }
}
