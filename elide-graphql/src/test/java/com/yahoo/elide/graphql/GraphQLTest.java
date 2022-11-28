/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.dictionary.ArgumentType;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.type.ClassType;
import example.Address;
import example.Author;
import example.Book;
import example.ParameterizedExample;
import example.Pseudonym;
import example.Publisher;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap for GraphQL tests.
 */
public abstract class GraphQLTest {
    protected EntityDictionary dictionary;
    protected Injector injector;

    public GraphQLTest() {
        Map<String, Class<? extends Check>> checks = new HashMap<>();
        checks.put("Prefab.Role.All", com.yahoo.elide.core.security.checks.prefab.Role.ALL.class);
        injector = mock(Injector.class);
        when(injector.instantiate(any())).thenCallRealMethod();

        dictionary = EntityDictionary.builder()
                .injector(injector)
                .checks(checks).build();

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Pseudonym.class);
        dictionary.bindEntity(Address.class);
        dictionary.bindEntity(ParameterizedExample.class);

        dictionary.addArgumentToAttribute(ClassType.of(ParameterizedExample.class), "attribute",
                new ArgumentType("argument", ClassType.STRING_TYPE, "defaultValue"));
        dictionary.addArgumentToEntity(ClassType.of(ParameterizedExample.class),
                new ArgumentType("entityArgument", ClassType.STRING_TYPE, "defaultArgValue"));
    }
}
