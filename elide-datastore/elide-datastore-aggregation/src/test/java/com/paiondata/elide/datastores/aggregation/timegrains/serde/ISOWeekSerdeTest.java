/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.paiondata.elide.datastores.aggregation.timegrains.ISOWeek;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ISOWeekSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Test
    public void testDateSerialize() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);

        ISOWeek expectedDate = new ISOWeek(localDate);
        Serde serde = new ISOWeek.ISOWeekSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals("2020-01-06", actual);
    }

    @Test
    public void testDateDeserializeString() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);

        ISOWeek expectedDate = new ISOWeek(localDate);
        Serde serde = new ISOWeek.ISOWeekSerde();
        Object actualDate = serde.deserialize(ISOWeek.class, "2020-01-06");
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestampNotMonday() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        ISOWeek timestamp = new ISOWeek(localDate);
        Serde serde = new ISOWeek.ISOWeekSerde();
        assertThrows(IllegalArgumentException.class, () ->
            serde.deserialize(ISOWeek.class, timestamp)
        );
    }

    @Test
    public void testDeserializeOffsetDateTime() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);
        ISOWeek expectedDate = new ISOWeek(localDate);

        OffsetDateTime dateTime = OffsetDateTime.of(2020, 01, 06, 00, 00, 00, 00, ZoneOffset.UTC);

        Serde serde = new ISOWeek.ISOWeekSerde();
        Object actualDate = serde.deserialize(ISOWeek.class, dateTime);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeOffsetDateTimeNotMonday() {
        OffsetDateTime dateTime = OffsetDateTime.of(2020, 01, 01, 00, 00, 00, 00, ZoneOffset.UTC);

        Serde serde = new ISOWeek.ISOWeekSerde();
        assertThrows(IllegalArgumentException.class, () ->
                serde.deserialize(ISOWeek.class, dateTime)
        );
    }

    @Test
    public void testDeserializeTimestamp() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);
        ISOWeek expectedDate = new ISOWeek(localDate);
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new ISOWeek.ISOWeekSerde();
        Object actualDate = serde.deserialize(ISOWeek.class, timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() {

        String dateInString = "January-2020-01";
        Serde serde = new ISOWeek.ISOWeekSerde();
        assertThrows(DateTimeParseException.class, () ->
            serde.deserialize(ISOWeek.class, dateInString)
        );
    }
}
