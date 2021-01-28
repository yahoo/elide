/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.timegrains.Minute;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MinuteSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Test
    public void testDateSerialize() throws ParseException {

        String expected = "2020-01-01T01:18";
        Minute expectedDate = new Minute(LocalDateTime.from(formatter.parse(expected)));
        Serde serde = new Minute.MinuteSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserializeString() throws ParseException {

        String dateInString = "2020-01-01T01:18";
        Minute expectedDate = new Minute(LocalDateTime.from(formatter.parse(dateInString)));
        String actual = "2020-01-01T01:18";
        Serde serde = new Minute.MinuteSerde();
        Object actualDate = serde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {

        String dateInString = "2020-01-01T01:18";
        Minute expectedDate = new Minute(LocalDateTime.from(formatter.parse(dateInString)));
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new Minute.MinuteSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "00:18 2020-01-01";
        Serde serde = new Minute.MinuteSerde();
        assertThrows(IllegalArgumentException.class, () -> {
            serde.deserialize(dateInString);
        });
    }
}
