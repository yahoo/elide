/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class EntityProjectionMakerTest {
    private EntityDictionary dictionary;

    @BeforeTest
    public void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Editor.class);
    }

    @Test
    public void testRootCollectionNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/book";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Book.class)
                .dictionary(dictionary)
                .attribute(Attribute.builder().name("title").type(String.class).build())
                .attribute(Attribute.builder().name("genre").type(String.class).build())
                .attribute(Attribute.builder().name("language").type(String.class).build())
                .attribute(Attribute.builder().name("publishDate").type(long.class).build())
                .build();

        EntityProjection actual = maker.make(path);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testNestedCollectionNoQueryParams() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        String path = "/author/1/books/3/publisher";

        EntityProjectionMaker maker = new EntityProjectionMaker(dictionary, queryParams);

        EntityProjection expected = EntityProjection.builder()
                .type(Author.class)
                .dictionary(dictionary)
                .relationship("books", EntityProjection.builder()
                        .dictionary(dictionary)
                        .type(Book.class)
                        .relationship("publisher", EntityProjection.builder()
                                .dictionary(dictionary)
                                .type(Publisher.class)
                                .attribute(Attribute.builder().name("name").type(String.class).build())
                                .attribute(Attribute.builder().name("updateHookInvoked").type(boolean.class).build())
                                .build())
                        .build())
                .build();

        EntityProjection actual = maker.make(path);

        Assert.assertEquals(actual, expected);
    }
}
