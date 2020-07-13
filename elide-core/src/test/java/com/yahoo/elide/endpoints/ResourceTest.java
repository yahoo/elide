/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.generated.parsers.CoreBaseVisitor;
import com.yahoo.elide.parsers.JsonApiParser;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

/**
 * The type Config resource test.
 */
public class ResourceTest {

    @Test
    public void verifyParseRelationship() {
        assertNull(new CoreBaseVisitor().visit(parse("parent/123/relationships/children")));
    }

    @Test
    public void verifyParseRelation() {
        assertNull(new CoreBaseVisitor().visit(parse("company/123/cities/2/relationships/states/1")));
    }

    @Test
    public void verifyBase64ID() {
        assertNull(new CoreBaseVisitor().visit(parse(
                "company/QWRkcmVzcyhudW1iZXI9MCwgc3RyZWV0PUJ1bGxpb24gQmx2ZCwgemlwQ29kZT00MDEyMSk=")));
    }

    @Test
    public void verifyURLEncodedID() {
        assertNull(new CoreBaseVisitor().visit(parse(
                "company/abcdef%201234")));
    }

    @Test
    public void parseFailRelationship() {
        assertThrows(
                ParseCancellationException.class,
                () -> new CoreBaseVisitor().visit(parse("company/123/relationships")));
    }

    @Test
    public void parseFailRelationshipCollection() {
        assertThrows(
                ParseCancellationException.class,
                () -> new CoreBaseVisitor().visit(parse("company/relationships")));
    }

    @Test
    public void parseFailure() {
        assertThrows(
                ParseCancellationException.class,
                () -> new CoreBaseVisitor().visit(parse("company/123|apps/2/links/foo")));
    }

    @Test
    public void wrongPathSeparator() {
        assertThrows(
                ParseCancellationException.class,
                () -> new CoreBaseVisitor().visit(parse("company\\123")));
    }

    private static ParseTree parse(String path) {
        return JsonApiParser.parse(path);
    }
}
