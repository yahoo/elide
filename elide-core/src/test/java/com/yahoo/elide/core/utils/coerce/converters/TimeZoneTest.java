/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.TimeZone;

public class TimeZoneTest {

    @Test
    public void testTimeZoneSerialize() {

        TimeZone timezone = TimeZone.getTimeZone("EST");
        String expected = "EST";
        TimeZoneSerde timeZoneSerde = new TimeZoneSerde();
        Object actual = timeZoneSerde.serialize(timezone);
        assertEquals(expected, actual);
    }

    @Test
    public void testTimeZoneDeserialize() {
        TimeZone expectedTimeZone = TimeZone.getTimeZone("EST");
        String actual = "EST";
        TimeZoneSerde timeZoneSerde = new TimeZoneSerde();
        Object actualTimeZone = timeZoneSerde.deserialize(actual);
        assertEquals(expectedTimeZone, actualTimeZone);
    }
}
