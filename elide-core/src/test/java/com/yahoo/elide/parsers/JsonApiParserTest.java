/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class JsonApiParserTest {

    @Test
    public void testNormalizePath() {

        String normalizedPath;

        normalizedPath = JsonApiParser.normalizePath("books/1/author");
        assertEquals(normalizedPath, "books/1/author", "it does not change normalized paths");

        normalizedPath = JsonApiParser.normalizePath("/books");
        assertEquals(normalizedPath, "books", "it removes leading path separators");

        normalizedPath = JsonApiParser.normalizePath("books/");
        assertEquals(normalizedPath, "books", "it removes trailing path separators");

        normalizedPath = JsonApiParser.normalizePath("///books///1///author////");
        assertEquals(normalizedPath, "books/1/author", "it deduplicates path separators");
    }
}
