/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.timegrains.SimpleDate;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SimpleDateTest {

    @Test
    public void testDateSerialize() throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String expected = "2020-01-01";
        SimpleDate expectedDate = new SimpleDate(formatter.parse(expected));
        //Date expectedDate = new Date();
        SimpleDateSerde dateSerde = new SimpleDateSerde();
        Object actual = dateSerde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserialize() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateInString = "2020-01-01";
        SimpleDate expectedDate = new SimpleDate(formatter.parse(dateInString));
        //Date expectedDate = new Date();
        String actual = "2020-01-01";
        SimpleDateSerde dateSerde = new SimpleDateSerde();
        Object actualDate = dateSerde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }
}
