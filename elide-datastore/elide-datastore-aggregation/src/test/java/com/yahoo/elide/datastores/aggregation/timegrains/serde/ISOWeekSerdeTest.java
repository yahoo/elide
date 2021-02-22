/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.timegrains.ISOWeek;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ISOWeekSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Test
    public void testDateSerialize() throws ParseException {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);

        ISOWeek expectedDate = new ISOWeek(localDate);
        Serde serde = new ISOWeek.ISOWeekSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals("2020-01-06", actual);
    }

    @Test
    public void testDateDeserializeString() throws ParseException {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);

        ISOWeek expectedDate = new ISOWeek(localDate);
        Serde serde = new ISOWeek.ISOWeekSerde();
        Object actualDate = serde.deserialize("2020-01-06");
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestampNotMonday() throws ParseException {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        ISOWeek timestamp = new ISOWeek(localDate);
        Serde serde = new ISOWeek.ISOWeekSerde();
        assertThrows(IllegalArgumentException.class, () ->
            serde.deserialize(timestamp)
        );
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);
        ISOWeek expectedDate = new ISOWeek(localDate);
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new ISOWeek.ISOWeekSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "January-2020-01";
        Serde serde = new ISOWeek.ISOWeekSerde();
        assertThrows(DateTimeParseException.class, () ->
            serde.deserialize(dateInString)
        );
    }
}
