/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.jsonapi;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.parsers.JsonApiParser;
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

        EntityProjection actual = maker.visit(JsonApiParser.parse(path));

        Assert.assertEquals(actual, expected);
    }
}
