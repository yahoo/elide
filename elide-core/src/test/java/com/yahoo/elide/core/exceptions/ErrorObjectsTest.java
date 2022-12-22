/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ErrorObjectsTest {
    @Test
    public void testAddErrorLast() {
        assertThrows(UnsupportedOperationException.class, () -> {
            ErrorObjects errorObj = new ErrorObjects.ErrorObjectsBuilder()
                    .with("foo", "bar")
                    .addError().build();
        });
    }

    @Test
    public void testAddErrorFirst() {
        ErrorObjects errorObj = new ErrorObjects.ErrorObjectsBuilder()
                .addError()
                .with("foo", "bar")
                .build();

        assertEquals(1, errorObj.getErrors().size());
        assertEquals("bar", errorObj.getErrors().get(0).get("foo"));
    }
}
