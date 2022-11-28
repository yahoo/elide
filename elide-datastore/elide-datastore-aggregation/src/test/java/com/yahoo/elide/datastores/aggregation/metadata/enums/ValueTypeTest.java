/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.aggregation.timegrains.Time;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ValueTypeTest {

    @BeforeAll
    public static void init() {
        CoerceUtil.register(Time.class, new Time.TimeSerde());
    }

    @Test
    public void testValidTimeValues() {
        assertTrue(ValueType.TIME.matches("2017"));
        assertTrue(ValueType.TIME.matches("2017-01"));
        assertTrue(ValueType.TIME.matches("2017-01-01"));
        assertTrue(ValueType.TIME.matches("2017-01-01T01"));
        assertTrue(ValueType.TIME.matches("2017-01-01T01:00"));
        assertTrue(ValueType.TIME.matches("2017-01-01T01:00:00"));
    }

    @Test
    public void testInvalidTimeValues() {
        assertFalse(ValueType.TIME.matches("foo"));
        assertFalse(ValueType.TIME.matches("2017;"));
        assertFalse(ValueType.COORDINATE.matches("DROP TABLE"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }

    @Test
    public void testValidDecimalValues() {
        assertTrue(ValueType.DECIMAL.matches("1"));
        assertTrue(ValueType.DECIMAL.matches("01"));
        assertTrue(ValueType.DECIMAL.matches("1.1"));
        assertTrue(ValueType.DECIMAL.matches("0.1"));
        assertTrue(ValueType.DECIMAL.matches(".1"));
        assertTrue(ValueType.DECIMAL.matches("100.100"));
        assertTrue(ValueType.DECIMAL.matches("-1"));
        assertTrue(ValueType.DECIMAL.matches("-01"));
        assertTrue(ValueType.DECIMAL.matches("-1.1"));
        assertTrue(ValueType.DECIMAL.matches("-0.1"));
        assertTrue(ValueType.DECIMAL.matches("-.1"));
        assertTrue(ValueType.DECIMAL.matches("-100.100"));
        assertTrue(ValueType.DECIMAL.matches("+1"));
        assertTrue(ValueType.DECIMAL.matches("+01"));
        assertTrue(ValueType.DECIMAL.matches("+1.1"));
        assertTrue(ValueType.DECIMAL.matches("+0.1"));
        assertTrue(ValueType.DECIMAL.matches("+.1"));
        assertTrue(ValueType.DECIMAL.matches("+100.100"));
    }

    @Test
    public void testInvalidDecimalValues() {
        assertFalse(ValueType.DECIMAL.matches("foo"));
        assertFalse(ValueType.DECIMAL.matches("01.00.00"));
        assertFalse(ValueType.DECIMAL.matches("1;"));
        assertFalse(ValueType.COORDINATE.matches("DROP TABLE"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }

    @Test
    public void testValidCoordinateValues() {
        assertTrue(ValueType.COORDINATE.matches("1"));
        assertTrue(ValueType.COORDINATE.matches("01"));
        assertTrue(ValueType.COORDINATE.matches("1.1"));
        assertTrue(ValueType.COORDINATE.matches("0.1"));
        assertTrue(ValueType.COORDINATE.matches("100.100"));
        assertTrue(ValueType.COORDINATE.matches("-1"));
        assertTrue(ValueType.COORDINATE.matches("-01"));
        assertTrue(ValueType.COORDINATE.matches("-1.1"));
        assertTrue(ValueType.COORDINATE.matches("-0.1"));
        assertTrue(ValueType.COORDINATE.matches("-100.100"));

        assertTrue(ValueType.COORDINATE.matches("1,1"));
        assertTrue(ValueType.COORDINATE.matches("01, 01"));
        assertTrue(ValueType.COORDINATE.matches("1.1,   1.1"));
        assertTrue(ValueType.COORDINATE.matches("-1.0, 1.0"));
    }

    @Test
    public void testInvalidCoordinateValues() {
        assertFalse(ValueType.COORDINATE.matches("1;"));
        assertFalse(ValueType.COORDINATE.matches(".101"));
        assertFalse(ValueType.COORDINATE.matches("+1.1"));
        assertFalse(ValueType.COORDINATE.matches("1.1, 1.1, 1.1"));
        assertFalse(ValueType.COORDINATE.matches("FOO"));
        assertFalse(ValueType.COORDINATE.matches("DROP TABLE"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }

    @Test
    public void testInvalidMoneyValues() {
        assertFalse(ValueType.MONEY.matches("foo"));
        assertFalse(ValueType.MONEY.matches("01.00.00"));
        assertFalse(ValueType.MONEY.matches("1;"));
        assertFalse(ValueType.COORDINATE.matches("DROP TABLE"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }

    @Test
    public void testValidMoneyValues() {
        assertTrue(ValueType.MONEY.matches("1"));
        assertTrue(ValueType.MONEY.matches("01"));
        assertTrue(ValueType.MONEY.matches("1.1"));
        assertTrue(ValueType.MONEY.matches("0.1"));
        assertTrue(ValueType.MONEY.matches(".1"));
        assertTrue(ValueType.MONEY.matches("100.100"));
        assertTrue(ValueType.MONEY.matches("-1"));
        assertTrue(ValueType.MONEY.matches("-01"));
        assertTrue(ValueType.MONEY.matches("-1.1"));
        assertTrue(ValueType.MONEY.matches("-0.1"));
        assertTrue(ValueType.MONEY.matches("-.1"));
        assertTrue(ValueType.MONEY.matches("-100.100"));
        assertTrue(ValueType.MONEY.matches("+1"));
        assertTrue(ValueType.MONEY.matches("+01"));
        assertTrue(ValueType.MONEY.matches("+1.1"));
        assertTrue(ValueType.MONEY.matches("+0.1"));
        assertTrue(ValueType.MONEY.matches("+.1"));
        assertTrue(ValueType.MONEY.matches("+100.100"));
    }

    @Test
    public void testValidNumberValues() {
        assertTrue(ValueType.INTEGER.matches("1"));
        assertTrue(ValueType.INTEGER.matches("01"));
        assertTrue(ValueType.INTEGER.matches("-1"));
        assertTrue(ValueType.INTEGER.matches("-01"));
        assertTrue(ValueType.INTEGER.matches("+1"));
        assertTrue(ValueType.INTEGER.matches("+01"));
    }

    @Test
    public void testInvalidNumberValues() {
        assertFalse(ValueType.INTEGER.matches("foo"));
        assertFalse(ValueType.INTEGER.matches("01.00"));
        assertFalse(ValueType.INTEGER.matches(".00"));
        assertFalse(ValueType.INTEGER.matches("1."));
        assertFalse(ValueType.INTEGER.matches("1;"));
        assertFalse(ValueType.COORDINATE.matches("DROP TABLE"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }

    @Test
    public void testValidBooleanValues() {
        assertTrue(ValueType.BOOLEAN.matches("1"));
        assertTrue(ValueType.BOOLEAN.matches("0"));
        assertTrue(ValueType.BOOLEAN.matches("true"));
        assertTrue(ValueType.BOOLEAN.matches("false"));
        assertTrue(ValueType.BOOLEAN.matches("tRuE"));
        assertTrue(ValueType.BOOLEAN.matches("fAlSe"));
    }

    @Test
    public void testInvalidBooleanValues() {
        assertFalse(ValueType.BOOLEAN.matches("foo"));
        assertFalse(ValueType.BOOLEAN.matches("01.00"));
        assertFalse(ValueType.BOOLEAN.matches(".00"));
        assertFalse(ValueType.BOOLEAN.matches("1."));
        assertFalse(ValueType.BOOLEAN.matches("1;"));
        assertFalse(ValueType.COORDINATE.matches("DROP TABLE"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }

    @Test
    public void testValidTextValues() {
        assertTrue(ValueType.TEXT.matches("1"));
        assertTrue(ValueType.TEXT.matches("01"));
        assertTrue(ValueType.TEXT.matches("abc"));
        assertTrue(ValueType.TEXT.matches("XYZ"));
        assertTrue(ValueType.TEXT.matches("123abc"));
        assertTrue(ValueType.TEXT.matches("___XYZ123abcABC"));
    }

    @Test
    public void testInvalidTextValues() {
        assertFalse(ValueType.TEXT.matches("foo&"));
        assertFalse(ValueType.TEXT.matches("01.00"));
        assertFalse(ValueType.TEXT.matches(".00"));
        assertFalse(ValueType.TEXT.matches("1."));
        assertFalse(ValueType.TEXT.matches("DROP TABLE"));
        assertFalse(ValueType.TEXT.matches("1;"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }

    @Test
    public void testValidIdValues() {
        assertTrue(ValueType.ID.matches("1"));
        assertTrue(ValueType.ID.matches("01"));
        assertTrue(ValueType.ID.matches("abc"));
        assertTrue(ValueType.ID.matches("XYZ"));
        assertTrue(ValueType.ID.matches("123abc"));
        assertTrue(ValueType.ID.matches("___XYZ123abcABC"));
    }

    @Test
    public void testInvalidIdValues() {
        assertFalse(ValueType.ID.matches("foo&"));
        assertFalse(ValueType.ID.matches("01.00"));
        assertFalse(ValueType.ID.matches(".00"));
        assertFalse(ValueType.ID.matches("1."));
        assertFalse(ValueType.ID.matches("DROP TABLE"));
        assertFalse(ValueType.ID.matches("1;"));
        //SQL Comment
        assertFalse(ValueType.ID.matches("--"));
    }
}
