/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import static org.testng.Assert.assertEquals;

import com.yahoo.elide.core.exceptions.InvalidAttributeException;

import org.apache.commons.beanutils.Converter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class ToEnumConverterTest {

    private Converter converter;

    public enum Seasons { WINTER, SPRING, SUMMER, FALL }

    @BeforeMethod
    public void setUp() throws Exception {
       this.converter = new ToEnumConverter();
    }

    @Test
    public void testIntToEnumConversion() throws Exception {

        assertEquals(converter.convert(Seasons.class, 0),  Seasons.WINTER,
                "Enum converter correctly converted int to enum");

        assertEquals(converter.convert(Seasons.class, 1),  Seasons.SPRING,
                "Enum converter correctly converted int to enum");
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testMissingNumberValueException() throws Exception {

        converter.convert(Seasons.class, 5);
    }

    @Test
    public void testStringToEnumConversion() throws Exception {

        assertEquals(converter.convert(Seasons.class, "SUMMER"),  Seasons.SUMMER,
                "Enum converter correctly converted String to enum");

        assertEquals(converter.convert(Seasons.class, "FALL"),  Seasons.FALL,
                "Enum converter correctly converted String to enum");
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testMissingStringValueException() throws Exception {

        converter.convert(Seasons.class, "AUTUMN");
    }

    @Test(expectedExceptions = InvalidAttributeException.class)
    public void testInvalidType() throws Exception {

        converter.convert(Seasons.class, 'A');
    }
}
