/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.timegrains.Week;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class WeekSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Test
    public void testDateSerialize() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 05, 00, 00, 00);

        Week expectedDate = new Week(localDate);
        Serde serde = new Week.WeekSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals("2020-01-05", actual);
    }

    @Test
    public void testDateDeserializeString() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 05, 00, 00, 00);
        Week expectedDate = new Week(localDate);
        Serde serde = new Week.WeekSerde();
        Object actualDate = serde.deserialize("2020-01-05");
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestampNotSunday() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 06, 00, 00, 00);

        Week expectedDate = new Week(localDate);
        Serde serde = new Week.WeekSerde();
        assertThrows(IllegalArgumentException.class, () ->
            serde.deserialize(expectedDate)
        );
    }

    @Test
    public void testDeserializeTimestamp() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 05, 00, 00, 00);

        Week expectedDate = new Week(localDate);
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new Week.WeekSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeOffsetDateTimeNotSunday() {
        OffsetDateTime dateTime = OffsetDateTime.of(2020, 1, 6, 0, 0, 0, 0, ZoneOffset.UTC);

        Serde serde = new Week.WeekSerde();
        assertThrows(IllegalArgumentException.class, () ->
                serde.deserialize(dateTime)
        );
    }

    @Test
    public void testDeserializeOffsetDateTime() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 05, 00, 00, 00);

        Week expectedDate = new Week(localDate);

        OffsetDateTime dateTime = OffsetDateTime.of(2020, 1, 5, 0, 0, 0, 0, ZoneOffset.UTC);
        Serde serde = new Week.WeekSerde();
        Object actualDate = serde.deserialize(dateTime);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() {

        String dateInString = "January-2020-01";
        Serde serde = new Week.WeekSerde();
        assertThrows(DateTimeParseException.class, () ->
            serde.deserialize(dateInString)
        );
    }
}
