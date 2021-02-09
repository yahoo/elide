/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class URLSerdeTest {

    @Test
    public void testURLSerialize() throws MalformedURLException {

        URL url = new URL("https://elide.io");
        String expected = "https://elide.io";
        URLSerde urlSerde = new URLSerde();
        Object actual = urlSerde.serialize(url);
        assertEquals(expected, actual);
    }

    @Test
    public void testURLDeserialize() throws MalformedURLException {
        URL expectedURL = new URL("https://elide.io");
        String actual = "https://elide.io";
        URLSerde urlSerde = new URLSerde();
        Object actualURL = urlSerde.deserialize(actual);
        assertEquals(expectedURL, actualURL);
    }
}
