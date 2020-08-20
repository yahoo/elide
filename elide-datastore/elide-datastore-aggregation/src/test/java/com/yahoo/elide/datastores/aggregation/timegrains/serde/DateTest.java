/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.datastores.aggregation.timegrains.Date;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateTest {

    @Test
    public void testDateSerialize() throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String expected = "2020-01-01";
        Date expectedDate = new Date(formatter.parse(expected));
        //Date expectedDate = new Date();
        DateSerde dateSerde = new DateSerde();
        Object actual = dateSerde.serialize(expectedDate);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateDeserialize() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateInString = "2020-01-01";
        Date expectedDate = new Date(formatter.parse(dateInString));
        //Date expectedDate = new Date();
        String actual = "2020-01-01";
        DateSerde dateSerde = new DateSerde();
        Object actualDate = dateSerde.deserialize(actual);
        assertEquals(expectedDate, actualDate);
    }
}
