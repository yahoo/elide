/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.utils.coerce.converters.EpochToDateConverter;
import com.paiondata.elide.core.utils.coerce.converters.ISO8601DateSerde;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.TimeZone;

/**
 * Test for Serdes.
 */
class SerdesTest {

    @Test
    void mutate() {
        Serdes serdes = Serdes.builder().withDefaults().build();
        Serdes mutated = serdes.mutate().withEpochDates().build();
        assertNotEquals(serdes.size(), mutated.size());
    }

    @Test
    void clear() {
        Serdes serdes = Serdes.builder().withDefaults().clear().build();
        assertEquals(0, serdes.size());
    }

    @Test
    void entry() {
        Serdes serdes = Serdes.builder().entry(Date.class, new EpochToDateConverter<>(Date.class)).build();
        assertEquals(1, serdes.size());
        serdes.values().forEach(serde -> {
            assertTrue(serde instanceof EpochToDateConverter);
        });
    }

    @Test
    void withISO8601Dates() {
        Serdes serdes = Serdes.builder().withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")).build();
        assertEquals(4, serdes.size());
        serdes.values().forEach(serde -> {
            assertTrue(serde instanceof ISO8601DateSerde);
        });
    }

    @Test
    void withEpochDatesDates() {
        Serdes serdes = Serdes.builder().withEpochDates().build();
        assertEquals(4, serdes.size());
        serdes.values().forEach(serde -> {
            assertTrue(serde instanceof EpochToDateConverter);
        });
    }
}
