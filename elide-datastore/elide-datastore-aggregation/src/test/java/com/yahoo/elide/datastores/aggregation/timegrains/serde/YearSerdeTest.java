/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.datastores.aggregation.timegrains.Year;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class YearSerdeTest {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy");

    @Test
    public void testDateSerialize() throws ParseException {

        String expected = "2020";
        Year expectedDate = new  Year(formatter.parse(expected));
        YearSerde dateSerde = new YearSerde();
        Object actual = dateSerde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserialize() throws ParseException {

        String dateInString = "2020";
        Year expectedDate = new  Year(formatter.parse(dateInString));
        String actual = "2020";
        YearSerde dateSerde = new YearSerde();
        Object actualDate = dateSerde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {

        String dateInString = "2020";
        Year expectedDate = new Year(formatter.parse(dateInString));
        Timestamp timestamp = new Timestamp(formatter.parse(dateInString).getTime());
        YearSerde dateSerde = new YearSerde();
        Object actualDate = dateSerde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "January";
        YearSerde dateSerde = new YearSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            dateSerde.deserialize(dateInString);
        });
    }
}
