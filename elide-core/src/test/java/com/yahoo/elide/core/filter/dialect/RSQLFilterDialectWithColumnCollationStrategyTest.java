/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * Tests RSQLFilterDialect
 */
public class RSQLFilterDialectWithColumnCollationStrategyTest {
    private static RSQLFilterDialect dialect;

    @BeforeAll
    public static void init() {
        EntityDictionary dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dialect = new RSQLFilterDialect(dictionary, new CaseSensitivityStrategy.UseColumnCollation());
    }

    @Test
    public void testTypedExpressionParsing() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book]",
                "title==*foo*;title!=bar*;(genre=in=(sci-fi,action),publishDate>123)"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams);

        assertEquals(1, expressionMap.size());
        assertEquals(
                "((book.title INFIX [foo] AND NOT (book.title PREFIX [bar])) "
                        + "AND (book.genre IN [sci-fi, action] OR book.publishDate GT [123]))",
                expressionMap.get("book").toString()
        );
    }

    @Test
    public void testGlobalExpressionParsing() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==*foo*;authors.name==Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("(book.title INFIX [foo] AND book.authors.name IN [Hemingway])", expression.toString());
    }

    @Test
    public void testEqualOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("book.title IN [Hemingway]", expression.toString());
    }

    @Test
    public void testNotEqualOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title!=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("NOT (book.title IN [Hemingway])", expression.toString());
    }

    @Test
    public void testInOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=in=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("book.title IN [Hemingway]", expression.toString());
    }

    @Test
    public void testOutOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=out=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("NOT (book.title IN [Hemingway])", expression.toString());
    }

    @Test
    public void testNumericComparisonOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "(publishDate=gt=5,publishDate=ge=5,publishDate=lt=10,publishDate=le=10);"
                        + "(publishDate>5,publishDate>=5,publishDate<10,publishDate<=10)"

        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals(
                "((((book.publishDate GT [5] OR book.publishDate GE [5]) "
                        + "OR book.publishDate LT [10]) OR book.publishDate LE [10]) AND "
                        + "(((book.publishDate GT [5] OR book.publishDate GE [5]) OR "
                        + "book.publishDate LT [10]) OR book.publishDate LE [10]))",
                expression.toString()
        );
    }

    @Test
    public void testSubstringOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==*Hemingway*,title==*Hemingway,title==Hemingway*"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals(
                "((book.title INFIX [Hemingway] OR book.title POSTFIX [Hemingway]) OR book.title PREFIX [Hemingway])",
                expression.toString()
        );
    }

    @Test
    public void testExpressionGrouping() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==foo;(title==bar;title==baz)"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("(book.title IN [foo] AND (book.title IN [bar] AND book.title IN [baz]))", expression.toString());
    }

    @Test
    public void testIsnullOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=true"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("book.title ISNULL []", expression.toString());
    }

    @Test
    public void testIsnullOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=1"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("book.title ISNULL []", expression.toString());
    }

    @Test
    public void testNotnullOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=false"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("book.title NOTNULL []", expression.toString());
    }

    @Test
    public void testNotnullOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=0"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        assertEquals("book.title NOTNULL []", expression.toString());
    }
}
