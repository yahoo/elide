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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class HourSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    @Test
    public void testDateSerialize() {

        String expected = "2020-01-01T01";
        Hour expectedDate = new Hour(LocalDateTime.from(formatter.parse(expected)));
        Serde serde = new Hour.HourSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserializeString() {

        String dateInString = "2020-01-01T01";
        Hour expectedDate = new Hour(LocalDateTime.from(formatter.parse(dateInString)));
        Serde serde = new Hour.HourSerde();
        Object actualDate = serde.deserialize(dateInString);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() {

        String dateInString = "2020-01-01T01";
        Hour expectedDate = new Hour(LocalDateTime.from(formatter.parse(dateInString)));
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new Hour.HourSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeOffsetDateTime() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 01, 00, 00);
        Hour expectedDate = new Hour(localDate);
        OffsetDateTime dateTime = OffsetDateTime.of(2020, 01, 01, 01, 00, 00, 00, ZoneOffset.UTC);

        Serde serde = new Hour.HourSerde();
        Object actualDate = serde.deserialize(dateTime);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() {

        String dateInString = "00 2020-01-01";
        Serde serde = new Hour.HourSerde();
        assertThrows(DateTimeParseException.class, () ->
            serde.deserialize(dateInString)
        );
    }
}
