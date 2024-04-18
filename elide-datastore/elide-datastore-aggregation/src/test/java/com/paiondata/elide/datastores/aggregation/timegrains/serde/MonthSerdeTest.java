/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.paiondata.elide.datastores.aggregation.timegrains.Month;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class MonthSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

    @Test
    public void testDateSerialize() {

        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Month expectedDate = new Month(localDate);
        Serde serde = new Month.MonthSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals("2020-01", actual);
    }

    @Test
    public void testDateDeserialize() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Month expectedDate = new Month(localDate);
        Serde serde = new Month.MonthSerde();
        Object actualDate = serde.deserialize(Month.class, "2020-01");
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Month expectedDate = new Month(localDate);
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new Month.MonthSerde();
        Object actualDate = serde.deserialize(Month.class, timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeOffsetDateTime() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Month expectedDate = new Month(localDate);

        OffsetDateTime dateTime = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Serde serde = new Month.MonthSerde();
        Object actualDate = serde.deserialize(Month.class, dateTime);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() {
        String dateInString = "January-2020";
        Serde serde = new Month.MonthSerde();
        assertThrows(DateTimeParseException.class, () ->
            serde.deserialize(Month.class, dateInString)
        );
    }
}
