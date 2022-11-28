/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.timegrains.Time;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class TimeSerdeTest {

    private static final String YEAR = "2020";
    private static final String MONTH = "2020-01";
    private static final String DATE = "2020-01-01";
    private static final String HOUR = "2020-01-01T00";
    private static final String MINUTE = "2020-01-01T00:00";
    private static final String SECOND = "2020-01-01T00:00:00";

    @Test
    public void testTimeDeserializeYear() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Time expectedDate = new Time(localDate, true, false, false, false, false, false, (unused) -> "");
        Serde serde = new Time.TimeSerde();
        Object actualDate = serde.deserialize(YEAR);
        assertEquals(expectedDate, actualDate);
        assertEquals(YEAR, serde.serialize(actualDate));
    }

    @Test
    public void testTimeDeserializeMonth() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Time expectedDate = new Time(localDate, true, true, false, false, false, false, (unused) -> "");
        Serde serde = new Time.TimeSerde();
        Object actualDate = serde.deserialize(MONTH);
        assertEquals(expectedDate, actualDate);
        assertEquals(MONTH, serde.serialize(actualDate));
    }

    @Test
    public void testTimeDeserializeDate() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Time expectedDate = new Time(localDate, true, true, true, false, false, false, (unused) -> "");
        Serde serde = new Time.TimeSerde();
        Object actualDate = serde.deserialize(DATE);
        assertEquals(expectedDate, actualDate);
        assertEquals(DATE, serde.serialize(actualDate));
    }

    @Test
    public void testTimeDeserializeHour() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Time expectedDate = new Time(localDate, true, true, true, true, true, true, (unused) -> "");
        Serde serde = new Time.TimeSerde();
        Object actualDate = serde.deserialize(HOUR);
        assertEquals(expectedDate, actualDate);
        assertEquals(HOUR, serde.serialize(actualDate));
    }

    @Test
    public void testTimeDeserializeMinute() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Time expectedDate = new Time(localDate, true, true, true, true, true, true, (unused) -> "");
        Serde serde = new Time.TimeSerde();
        Object actualDate = serde.deserialize(MINUTE);
        assertEquals(expectedDate, actualDate);
        assertEquals(MINUTE, serde.serialize(actualDate));
    }

    @Test
    public void testTimeDeserializeSecond() {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);
        Time expectedDate = new Time(localDate, true, true, true, true, true, true, (unused) -> "");
        Serde serde = new Time.TimeSerde();
        Object actualDate = serde.deserialize(SECOND);
        assertEquals(expectedDate, actualDate);
        assertEquals(SECOND, serde.serialize(actualDate));
    }

    @Test
    public void testInvalidDeserialization() {
        Serde serde = new Time.TimeSerde();
        assertThrows(IllegalArgumentException.class, () ->
            serde.deserialize("2020R1")
        );
    }
}
