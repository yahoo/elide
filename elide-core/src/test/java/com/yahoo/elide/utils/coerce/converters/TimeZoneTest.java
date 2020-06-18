/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.TimeZone;

public class TimeZoneTest {

    @Test
    public void testGraphQLTimeZoneSerialize() {

        TimeZone timezone = TimeZone.getTimeZone("EST");
        String expected = "EST";
        TimeZoneSerde timeZoneScalar = new TimeZoneSerde();
        Object actual = timeZoneScalar.serialize(timezone);
        assertEquals(expected, actual);
    }

    @Test
    public void testGraphQLTimeZoneDeserialize() {
        TimeZone expectedTimeZone = TimeZone.getTimeZone("EST");
        String actual = "EST";
        TimeZoneSerde timeZoneScalar = new TimeZoneSerde();
        Object actualTimeZone = timeZoneScalar.deserialize(actual);
        assertEquals(expectedTimeZone, actualTimeZone);
    }
}
