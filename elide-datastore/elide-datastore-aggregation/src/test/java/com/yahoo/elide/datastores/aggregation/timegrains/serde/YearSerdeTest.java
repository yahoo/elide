/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.datastores.aggregation.timegrains.Year;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class YearSerdeTest {
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");

    @Test
    public void testDateSerialize() throws ParseException {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);

        Year expectedDate = new Year(localDate);
        Serde serde = new Year.YearSerde();
        Object actual = serde.serialize(expectedDate);
        assertEquals("2020", actual);
    }

    @Test
    public void testDateDeserialize() throws ParseException {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);

        Year expectedDate = new Year(localDate);
        Serde serde = new Year.YearSerde();
        Object actualDate = serde.deserialize("2020");
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeTimestamp() throws ParseException {
        LocalDateTime localDate = LocalDateTime.of(2020, java.time.Month.of(01), 01, 00, 00, 00);

        Year expectedDate = new Year(localDate);
        Timestamp timestamp = new Timestamp(expectedDate.getTime());
        Serde serde = new Year.YearSerde();
        Object actualDate = serde.deserialize(timestamp);
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testDeserializeDateInvalidFormat() throws ParseException {

        String dateInString = "January";
        Serde serde = new Year.YearSerde();
        assertThrows(DateTimeParseException.class, () ->
            serde.deserialize(dateInString)
        );
    }
}
