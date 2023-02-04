/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.timegrains.Quarter;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class QuarterSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

    @Test
    public void testDateSerialize() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Quarter expectedDate = new Quarter(localDate);
        Serde serde = new Quarter.QuarterSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals("2020-01", actual);
    }

    @Test
    public void testDateDeserialize() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Quarter expectedDate = new Quarter(localDate);
        Serde serde = new Quarter.QuarterSerde();
        Object actualDate = serde.deserialize("2020-01");
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestampNotQuarterMonth() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(02), 01, 00, 00, 00);
        Quarter quarter = new Quarter(localDate);
        Serde serde = new Quarter.QuarterSerde();
        assertThrows(IllegalArgumentException.class, () ->
            serde.deserialize(quarter)
        );
    }

    @Test
    public void testDeserializeTimestamp() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Quarter expectedDate = new Quarter(localDate);
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new Quarter.QuarterSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeOffsetDateTimeNotQuarterMonth() {
        OffsetDateTime dateTime = OffsetDateTime.of(2020, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        Serde serde = new Quarter.QuarterSerde();
        assertThrows(IllegalArgumentException.class, () ->
                serde.deserialize(dateTime)
        );
    }

    @Test
    public void testDeserializeOffsetDateTime() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Quarter expectedDate = new Quarter(localDate);
        OffsetDateTime dateTime = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Serde serde = new Quarter.QuarterSerde();
        Object actualDate = serde.deserialize(dateTime);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() {

        String dateInString = "January-2020";
        Serde serde = new Quarter.QuarterSerde();
        assertThrows(DateTimeParseException.class, () ->
            serde.deserialize(dateInString)
        );
    }
}
