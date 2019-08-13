/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql.elements;

import com.yahoo.elide.contrib.testhelpers.example.Author;
import com.yahoo.elide.contrib.testhelpers.example.Book;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ObjectValueWithVariableTest {

    @Test
    public void testObjectValueSerialization() {
        Book book = Book.builder()
                .id(1)
                .title("my new book!")
                .author(Author.builder().id(2L).build())
                .build();


        Assert.assertEquals(
                new ObjectValueWithVariable(book).toGraphQLSpec(),
                "{id:1,title:\"my new book!\",authors:[{id:2}]}"
        );
    }
}
