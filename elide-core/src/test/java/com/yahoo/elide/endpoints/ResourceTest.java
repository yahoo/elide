/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.endpoints;

import com.yahoo.elide.generated.parsers.CoreBaseVisitor;
import com.yahoo.elide.parsers.JsonApiParser;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.testng.annotations.Test;

/**
 * The type Config resource test.
 */
public class ResourceTest {

    @Test
    public void verifyParseRelationship() {
        new CoreBaseVisitor().visit(parse("parent/123/relationships/children"));
    }

    @Test
    public void verifyParseRelation() {
        new CoreBaseVisitor().visit(parse("company/123/cities/2/relationships/states/1"));
    }

    @Test(expectedExceptions = ParseCancellationException.class)
    public void parseFailRelationship() {
        new CoreBaseVisitor().visit(parse("company/123/relationships"));
    }

    @Test(expectedExceptions = ParseCancellationException.class)
    public void parseFailRelationshipCollection() {
        new CoreBaseVisitor().visit(parse("company/relationships"));
    }

    @Test(expectedExceptions = ParseCancellationException.class)
    public void parseFailure() {
        new CoreBaseVisitor().visit(parse("company/123|apps/2/links/foo"));
    }

    @Test(expectedExceptions = ParseCancellationException.class)
    public void wrongPathSeparator() {
        new CoreBaseVisitor().visit(parse("company\\123"));
    }

    private static ParseTree parse(String path) {
        return JsonApiParser.parse(path);
    }
}
