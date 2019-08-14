/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import com.yahoo.elide.contrib.testhelpers.example.Author;
import com.yahoo.elide.contrib.testhelpers.example.Book;

import com.google.common.collect.Sets;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

public class ArrayValueWithVariableTest {

    @Test
    public void testArrayValueSerialization() {
        Assert.assertEquals(
                new ArrayValueWithVariable(
                        Arrays.asList(
                                new StringValue("1"),
                                new StringValue("2"),
                                new StringValue("3")
                        )
                ).toGraphQLSpec(),
                "[\"1\", \"2\", \"3\"]"
        );
    }
}
