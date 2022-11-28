/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import org.apache.commons.beanutils.Converter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ToEnumConverterTest {

    private Converter converter;

    public enum Seasons { WINTER, SPRING, SUMMER, FALL }

    @BeforeEach
    public void setUp() throws Exception {
       this.converter = new ToEnumConverter();
    }

    @Test
    public void testIntToEnumConversion() throws Exception {

        assertEquals(
                Seasons.WINTER,
                converter.convert(Seasons.class, 0),
                "Enum converter correctly converted int to enum");

        assertEquals(
                Seasons.SPRING,
                converter.convert(Seasons.class, 1),
                "Enum converter correctly converted int to enum");
    }

    @Test
    public void testMissingNumberValueException() throws Exception {

        assertThrows(InvalidAttributeException.class, () -> converter.convert(Seasons.class, 5));
    }

    @Test
    public void testStringToEnumConversion() throws Exception {

        assertEquals(
                Seasons.SUMMER,
                converter.convert(Seasons.class, "SUMMER"),
                "Enum converter correctly converted String to enum");

        assertEquals(
                Seasons.FALL,
                converter.convert(Seasons.class, "FALL"),
                "Enum converter correctly converted String to enum");
    }

    @Test
    public void testMissingStringValueException() throws Exception {

        assertThrows(InvalidAttributeException.class, () -> converter.convert(Seasons.class, "AUTUMN"));
    }

    @Test
    public void testInvalidType() throws Exception {

        assertThrows(InvalidAttributeException.class, () -> converter.convert(Seasons.class, 'A'));
    }
}
