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

public class ObjectValueWithVariableTest {

    @Test
    public void testObjectValueSerialization() {
        Author author = new Author();
        author.setId(2L);

        Book book = new Book();
        book.setId(1);
        book.setTitle("my new book!");
        book.setAuthors(Sets.newHashSet(author));


        Assert.assertEquals(
                new ObjectValueWithVariable(book).toGraphQLSpec(),
                "{id:1,title:\"my new book!\",authors:[{id:2}]}"
        );
    }
}
