/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.datastore.inmemory;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.TestRequestScope;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.type.ClassType;
import example.Book;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FilteredIteratorTest {

    @Test
    public void testFilteredResult() throws Exception {

        EntityDictionary dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);

        Book book1 = new Book();
        book1.setTitle("foo");
        Book book2 = new Book();
        book2.setTitle("bar");
        Book book3 = new Book();
        book3.setTitle("foobar");
        List<Book> books = List.of(book1, book2, book3);

        RSQLFilterDialect filterDialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

        FilterExpression expression =
                filterDialect.parse(ClassType.of(Book.class), new HashSet<>(), "title==*bar", NO_VERSION);

        RequestScope scope = new TestRequestScope(null, null, dictionary);

        Iterator<Book> bookIterator = new FilteredIterator<>(expression, scope, books.iterator());

        assertTrue(bookIterator.hasNext());
        assertEquals("bar", bookIterator.next().getTitle());
        assertTrue(bookIterator.hasNext());
        assertEquals("foobar", bookIterator.next().getTitle());
        assertFalse(bookIterator.hasNext());
    }

    @Test
    public void testEmptyResult() throws Exception {

        EntityDictionary dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);

        List<Book> books = List.of();

        RSQLFilterDialect filterDialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

        FilterExpression expression =
                filterDialect.parse(ClassType.of(Book.class), new HashSet<>(), "title==*bar", NO_VERSION);

        RequestScope scope = new TestRequestScope(null, null, dictionary);

        Iterator<Book> bookIterator = new FilteredIterator<>(expression, scope, books.iterator());


        assertFalse(bookIterator.hasNext());
        assertThrows(NoSuchElementException.class, () -> bookIterator.next());
    }
}
