/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.security.checks.Check;

import example.Address;
import example.Author;
import example.Book;
import example.Pseudonym;
import example.Publisher;
import org.testng.annotations.BeforeClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap for GraphQL tests.
 */
public class GraphQLTest {
    protected EntityDictionary dictionary;

    @BeforeClass
    public void init() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("allow all", com.yahoo.elide.security.checks.prefab.Role.ALL.class);

        dictionary = new EntityDictionary(checks);

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Pseudonym.class);
        dictionary.bindEntity(Address.class);
    }
}
