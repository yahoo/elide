/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.expression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InMemoryExecutionVerifierTest {

    private static EntityDictionary dictionary;

    @BeforeAll
    public static void init() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(Publisher.class);
    }

    @Test
    public void testNoInMemoryExecution() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        assertFalse(InMemoryExecutionVerifier.shouldExecuteInMemory(dictionary, expression));
    }

    @Test
    public void testFullInMemoryExecution() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Literary Fiction");

        FilterExpression finalExpression = new NotFilterExpression(new AndFilterExpression(dataStoreExpression, inMemoryExpression));

        assertTrue(InMemoryExecutionVerifier.shouldExecuteInMemory(dictionary, finalExpression));
    }

    @Test
    public void testPartialInMemoryExecution() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Literary Fiction");

        FilterExpression finalExpression = new NotFilterExpression(new OrFilterExpression(dataStoreExpression, inMemoryExpression));

        assertTrue(InMemoryExecutionVerifier.shouldExecuteInMemory(dictionary, finalExpression));
    }
}
