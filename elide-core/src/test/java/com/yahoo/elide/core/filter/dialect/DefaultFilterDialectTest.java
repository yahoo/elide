/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests filter query parameter parsing for the default Elide implementation (1.0 and 2.0)
 */
public class DefaultFilterDialectTest {

    static DefaultFilterDialect dialect;

    @BeforeAll
    public static void init() {
        EntityDictionary dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dialect = new DefaultFilterDialect(dictionary);
    }

    @Test
    public void testGlobalExpressionParsing() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        FilterExpression filterExpression = dialect.parseGlobalExpression("/author", queryParams);

        assertEquals(
                "(author.books.title IN [foo, bar, baz] AND author.name INFIX [Hemingway])",
                filterExpression.toString()
        );
    }

    @Test
    public void testTypedExpressionParsing() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[book.genre]",
                "scifi"
        );


        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams);

        assertEquals(2, expressionMap.size());
        assertEquals(expressionMap.get("book").toString(),
                "(book.title IN [foo, bar, baz] AND book.genre IN [scifi])"
        );
        assertEquals("author.name INFIX [Hemingway]", expressionMap.get("author").toString());
    }

    @Test
    public void testInvalidType() throws ParseException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[invalid.title][in]",
                "foo,bar,baz"
        );

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/invalid", queryParams));
    }

    @Test
    public void testInvalidAttribute() throws ParseException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book.invalid][in]",
                "foo,bar,baz"
        );

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/book", queryParams));
    }

    @Test
    public void testInvalidTypeQualifier() throws ParseException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        /* This is OK for global but not OK for subqueries */
        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/author", queryParams));
    }
}
