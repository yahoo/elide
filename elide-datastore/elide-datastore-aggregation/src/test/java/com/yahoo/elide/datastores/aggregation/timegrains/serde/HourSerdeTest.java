/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.timegrains.Hour;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HourSerdeTest {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH");

    @Test
    public void testDateSerialize() throws ParseException {

        String expected = "2020-01-01T01";
        Hour expectedDate = new Hour(formatter.parse(expected));
        Serde serde = new Hour.HourSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserializeString() throws ParseException {

        String dateInString = "2020-01-01T01";
        Date expectedDate = new Date(formatter.parse(dateInString).getTime());
        String actual = "2020-01-01T01:18";
        Serde serde = new Hour.HourSerde();
        Object actualDate = serde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {

        String dateInString = "2020-01-01T01";
        Hour expectedDate = new Hour(formatter.parse(dateInString));
        Timestamp timestamp = new Timestamp(formatter.parse(dateInString).getTime());
        Serde serde = new Hour.HourSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "00 2020-01-01";
        Serde serde = new Hour.HourSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            serde.deserialize(dateInString);
        });
    }
}
