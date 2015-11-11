/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerse;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CoerceUtilTest {

    public static enum Seasons { WINTER, SPRING }

    @Test
    public void testNoConversions() throws Exception {

        assertEquals(CoerceUtil.coerce(1, null), 1,
                "coerce returns value if target class null");

        assertEquals(CoerceUtil.coerce(null, Object.class), null,
                "coerce returns value if value is null");

        assertEquals(CoerceUtil.coerce(1, int.class), 1,
                "coerce returns value if value is assignable to target");
    }

    @Test
    public void testToEnumConversion() throws Exception {

        assertEquals(CoerceUtil.coerce(1, Seasons.class), Seasons.SPRING,
                "EnumConverter is called when target class is Enum");
    }

    @Test
    public void testBasicConversion() throws Exception {

        assertEquals(CoerceUtil.coerce(1, String.class), "1",
                "coerce converts int to String");

        assertEquals(CoerceUtil.coerce("1", Long.class), Long.valueOf("1"),
                "coerce converts String to Long");

        assertEquals(CoerceUtil.coerce("1", long.class), Long.valueOf("1").longValue(),
                "coerce converts String to long");

        assertEquals(CoerceUtil.coerce(1.0, int.class), 1,
                "coerce converts float to int");
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testError() throws Exception {

        CoerceUtil.coerce('A', Seasons.class);
    }
}
