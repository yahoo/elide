/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.google.common.collect.ImmutableList;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class FilterPredicatePushdownExtractorTest {

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
    public void testFullPredicateExtraction() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, expression);

        assertEquals(expression, extracted);
    }

    @Test
    public void testNoPredicateExtraction() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Literary Fiction");

        FilterExpression finalExpression = new NotFilterExpression(new AndFilterExpression(dataStoreExpression, inMemoryExpression));

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, finalExpression);

        assertNull(extracted);
    }

    @Test
    public void testPartialPredicateExtraction() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Literary Fiction");

        FilterExpression finalExpression = new NotFilterExpression(new OrFilterExpression(dataStoreExpression, inMemoryExpression));

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, finalExpression);

        assertEquals(((InPredicate) dataStoreExpression).negate(), extracted);
    }

    @Test
    public void testAndPartialPredicateExtraction() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression anotherDataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Science Fiction");

        FilterExpression expectedExpression =
                new AndFilterExpression(dataStoreExpression, anotherDataStoreExpression);

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Jack");

        FilterExpression finalExpression = new AndFilterExpression(new AndFilterExpression(dataStoreExpression, inMemoryExpression), anotherDataStoreExpression);

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, finalExpression);

        assertEquals(expectedExpression, extracted);
    }

    @Test
    public void testOrPartialPredicateExtraction() {
        FilterExpression dataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        FilterExpression anotherDataStoreExpression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Science Fiction");

        FilterExpression expectedExpression =
                new OrFilterExpression(dataStoreExpression, anotherDataStoreExpression);

        FilterExpression inMemoryExpression =
                new InPredicate(new Path(Book.class, dictionary, "editor.firstName"), "Jane");

        FilterExpression finalExpression = new OrFilterExpression(new AndFilterExpression(dataStoreExpression, inMemoryExpression), anotherDataStoreExpression);

        FilterExpression extracted = FilterPredicatePushdownExtractor.extractPushDownPredicate(dictionary, finalExpression);

        assertEquals(expectedExpression, extracted);
    }

    @Test
    public void testInvalidField() {
        InvalidValueException e = assertThrows(
                InvalidValueException.class,
                () -> new InPredicate(new Path(Book.class, dictionary, "badfield"), "Literary Fiction"));
        assertEquals("Invalid value: book does not contain the field badfield", e.getMessage());
    }

    @Test
    public void testPath() {
        Path path = new Path(Book.class, dictionary, "genre");

        ImmutableList<PathElement> pathElements = ImmutableList.of(
                new PathElement(Book.class, String.class, "genre"));

        assertEquals("example_Book", path.getAlias());
        assertEquals("genre", path.getFieldPath());
        assertEquals(pathElements, path.getPathElements());
        assertEquals(Optional.of(pathElements.get(0)), path.lastElement());
        assertEquals("[Book].genre", path.toString());

        path = new Path(Book.class, dictionary, "this.editor.firstName");

        pathElements = ImmutableList.of(
                new PathElement(Book.class, null, "this"),
                new PathElement(Book.class, Editor.class, "editor"),
                new PathElement(Editor.class, String.class, "firstName"));

        assertEquals("example_Book_editor", path.getAlias());
        assertEquals("this.editor.firstName", path.getFieldPath());
        assertEquals(pathElements, path.getPathElements());
        assertEquals(Optional.of(pathElements.get(2)), path.lastElement());
        assertEquals("[Book].this/[Book].editor/[Editor].firstName", path.toString());
    }
}
