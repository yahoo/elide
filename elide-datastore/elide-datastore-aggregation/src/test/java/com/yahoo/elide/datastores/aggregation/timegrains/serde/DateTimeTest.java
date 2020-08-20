/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.timegrains.DateTime;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateTimeTest {

    @Test
    public void testDateSerialize() throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String expected = "2020-01-01 01:00:00";
        DateTime expectedDate = new DateTime(formatter.parse(expected));
        DateTimeSerde dateSerde = new DateTimeSerde();
        Object actual = dateSerde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserialize() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateInString = "2020-01-01 01:00:00";
        DateTime expectedDate = new DateTime(formatter.parse(dateInString));
        String actual = "2020-01-01 01:00:00";
        DateTimeSerde dateTimeSerde = new DateTimeSerde();
        Object actualDate = dateTimeSerde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }
}
