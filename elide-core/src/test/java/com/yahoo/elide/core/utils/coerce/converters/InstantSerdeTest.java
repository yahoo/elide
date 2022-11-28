/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.time.Instant;

public class InstantSerdeTest {
    private final Serde<String, Instant> serde = new InstantSerde();

    @Test
    public void canDeserializeUtcIsoString() {
        final Instant instant = serde.deserialize("2019-06-01T09:42:55Z");

        assertEquals(1559382175, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
    }

    @Test
    public void canDeserializeOffsetIsoString() {
        final Instant instant = serde.deserialize("2019-06-01T10:42:55+01:00");

        assertEquals(1559382175, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
    }

    @Test
    public void canDeserializeSubSecondPrecisionUtcIsoString() {
        final Instant instant = serde.deserialize("2019-06-01T09:42:55.123Z");

        assertEquals(1559382175, instant.getEpochSecond());
        assertEquals(123000000, instant.getNano());
    }

    @Test
    public void canSerialize() {
        assertEquals(
            "2019-06-01T09:42:55Z",
            serde.serialize(Instant.ofEpochSecond(1559382175))
        );
    }

    @Test
    public void canSerializeSubSecondPrecision() {
        assertEquals(
            "2019-06-01T09:42:55.123Z",
            serde.serialize(Instant.ofEpochMilli(1559382175123L))
        );
    }

    @Test
    public void failsParsingWithIllegalArgumentException() {

        assertThrows(
            IllegalArgumentException.class,
            () -> serde.deserialize("2019-06-01T09:42:55.12X3Z")
        );

    }
}
