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
        System.out.println("testGraphQLTimeZoneSerialize " + timezone);
        String expected = "EST";
        TimeZoneSerde timeZoneScalar = new TimeZoneSerde();
        Object actualTimeZone = timeZoneScalar.serialize(timezone);
        assertEquals(expected, actualTimeZone);
    }

    @Test
    public void testGraphQLTimeZoneDeserialize() {
        TimeZone actualTimeZone = TimeZone.getTimeZone("EST");
        System.out.println(actualTimeZone);
        String actual = "EST";
        TimeZoneSerde timeZoneScalar = new TimeZoneSerde();
        Object expected = timeZoneScalar.deserialize(actual);
        System.out.println(expected);
        assertEquals(expected, actualTimeZone);
    }
}
