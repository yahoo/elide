/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.books.Address;
import com.yahoo.books.Author;
import com.yahoo.books.Book;
import com.yahoo.books.Pseudonym;
import com.yahoo.books.Publisher;
import com.yahoo.elide.core.EntityDictionary;
import org.testng.annotations.BeforeClass;

import java.util.Collections;

/**
 * Bootstrap for GraphQL tests
 */
public class AbstractGraphQLTest {
    protected EntityDictionary dictionary;

    @BeforeClass
    public void init() {
        dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Pseudonym.class);
        dictionary.bindEntity(Address.class);
    }
}
