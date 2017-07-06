/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.books.Address;
import com.yahoo.elide.books.Author;
import com.yahoo.elide.books.Book;
import com.yahoo.elide.books.Pseudonym;
import com.yahoo.elide.books.Publisher;
import com.yahoo.elide.core.EntityDictionary;
import org.testng.annotations.BeforeClass;

import java.util.Collections;

/**
 * Bootstrap for GraphQL tests
 */
public class GraphQLTest {
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
