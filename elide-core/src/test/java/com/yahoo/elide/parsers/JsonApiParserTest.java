/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import org.junit.jupiter.api.Test;

public class JsonApiParserTest {

    @Test
    public void testNormalizePath() {

        String normalizedPath;

        normalizedPath = JsonApiParser.normalizePath("books/1/author");
        assertEquals("books/1/author", normalizedPath,  "it does not change normalized paths");

        normalizedPath = JsonApiParser.normalizePath("/books");
        assertEquals("books", normalizedPath,  "it removes leading path separators");

        normalizedPath = JsonApiParser.normalizePath("books/");
        assertEquals("books", normalizedPath,  "it removes trailing path separators");

        normalizedPath = JsonApiParser.normalizePath("///books///1///author////");
        assertEquals("books/1/author", normalizedPath,  "it deduplicates path separators");

        normalizedPath = JsonApiParser.normalizePath("//books//1//author//");
        assertEquals("books/1/author", normalizedPath,  "it deduplicates path separators");
    }
}
