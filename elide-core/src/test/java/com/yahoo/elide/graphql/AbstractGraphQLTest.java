/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import example.Author;
import example.Book;
import org.testng.annotations.BeforeSuite;

import java.util.Collections;

/**
 * Bootstrap for GraphQL tests
 */
public class AbstractGraphQLTest {
    protected EntityDictionary dictionary;

    @BeforeSuite
    public void init() {
        dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
    }
}
