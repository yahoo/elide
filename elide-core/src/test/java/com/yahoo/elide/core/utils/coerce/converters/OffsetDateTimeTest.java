/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class OffsetDateTimeTest {

    @Test
    public void testGraphQLOffsetDateTimeSerialize() {
        OffsetDateTime offsetDateTime =
                OffsetDateTime.of(1995, 11, 2,
                        16, 45, 4, 56,
                        ZoneOffset.ofHoursMinutes(5, 30));
        String expected = "1995-11-02T16:45:04.000000056+05:30";
        OffsetDateTimeSerde offsetDateTimeScalar = new OffsetDateTimeSerde();
        Object actualDate = offsetDateTimeScalar.serialize(offsetDateTime);
        assertEquals(expected, actualDate);
    }

    @Test
    public void testGraphQLOffsetDateTimeDeserialize() {
        OffsetDateTime actualDate =
                OffsetDateTime.of(1995, 11, 2,
                        16, 45, 4, 56,
                        ZoneOffset.ofHoursMinutes(5, 30));
        String actual = "1995-11-02T16:45:04.000000056+05:30";
        OffsetDateTimeSerde offsetDateTimeScalar = new OffsetDateTimeSerde();
        Object expected = offsetDateTimeScalar.deserialize(actual);
        assertEquals(expected, actualDate);
    }

    @Test
    public void failsParsingWithIllegalArgumentException() {
        final OffsetDateTimeSerde offsetDateTimeScalar = new OffsetDateTimeSerde();
        assertThrows(
            IllegalArgumentException.class,
            () -> offsetDateTimeScalar.deserialize("2019-06-01T09:42:55.12X3Z")
        );
    }
}
