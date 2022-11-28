/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests out different JSONPath parsing results.
 */
public class SafeJsonPathTest {

    @Test
    public void testIntegerResult() {
        Integer count = AsyncQueryOperation.safeJsonPathLength("{ \"data\": [1,2,3] }", "data.length()");

        assertEquals(3, count);
    }

    @Test
    public void testArrayResult() {
        Integer count = AsyncQueryOperation.safeJsonPathLength("{ \"data\": [10] }", "data");

        assertEquals(10, count);
    }

    @Test
    public void testEmptyArrayResult() {
        Integer count = AsyncQueryOperation.safeJsonPathLength("{ \"data\": [] }", "data");

        assertEquals(0, count);
    }

    @Test
    public void testInvalidREsult() {
        assertThrows(IllegalStateException.class, () ->
                AsyncQueryOperation.safeJsonPathLength("{ \"data\": \"foo\" }", "data"));
    }
}
