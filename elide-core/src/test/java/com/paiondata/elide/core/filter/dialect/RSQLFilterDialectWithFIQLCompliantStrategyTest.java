/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.dialect;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests RSQLFilterDialect.
 */
public class RSQLFilterDialectWithFIQLCompliantStrategyTest {
    private static RSQLFilterDialect dialect;

    @BeforeAll
    public static void init() {
        EntityDictionary dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dialect = RSQLFilterDialect.builder()
                .dictionary(dictionary)
                .caseSensitivityStrategy(new CaseSensitivityStrategy.FIQLCompliant())
                .build();
    }

    static void add(Map<String, List<String>> params, String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    @Test
    public void testTypedExpressionParsing() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[book]",
                "title==*foo*;title!=bar*;(genre=in=(sci-fi,action),publishDate>123)"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals(
                "((book.title INFIX_CASE_INSENSITIVE [foo] AND NOT (book.title PREFIX_CASE_INSENSITIVE [bar])) "
                        + "AND (book.genre IN_INSENSITIVE [sci-fi, action] OR book.publishDate GT [123]))",
                expressionMap.get("book").toString()
        );
    }

    @Test
    public void testGlobalExpressionParsing() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title==*foo*;authors.name==Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("(book.title INFIX_CASE_INSENSITIVE [foo] AND book.authors.name IN_INSENSITIVE [Hemingway])", expression.toString());
    }

    @Test
    public void testEqualOperator() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title==Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title IN_INSENSITIVE [Hemingway]", expression.toString());
    }

    @Test
    public void testNotEqualOperator() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title!=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("NOT (book.title IN_INSENSITIVE [Hemingway])", expression.toString());
    }

    @Test
    public void testInOperator() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title=in=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title IN_INSENSITIVE [Hemingway]", expression.toString());
    }

    @Test
    public void testOutOperator() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title=out=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("NOT (book.title IN_INSENSITIVE [Hemingway])", expression.toString());
    }

    @Test
    public void testNumericComparisonOperator() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "(publishDate=gt=5,publishDate=ge=5,publishDate=lt=10,publishDate=le=10);"
                        + "(publishDate>5,publishDate>=5,publishDate<10,publishDate<=10)"

        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title==*Hemingway*,title==*Hemingway,title==Hemingway*"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals(
                "((book.title INFIX_CASE_INSENSITIVE [Hemingway] OR book.title POSTFIX_CASE_INSENSITIVE [Hemingway]) OR book.title PREFIX_CASE_INSENSITIVE [Hemingway])",
                expression.toString()
        );
    }

    @Test
    public void testExpressionGrouping() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title==foo;(title==bar;title==baz)"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("(book.title IN_INSENSITIVE [foo] AND (book.title IN_INSENSITIVE [bar] AND book.title IN_INSENSITIVE [baz]))", expression.toString());
    }

    @Test
    public void testIsnullOperatorBool() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title=isnull=true"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title ISNULL []", expression.toString());
    }

    @Test
    public void testIsnullOperatorInt() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title=isnull=1"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title ISNULL []", expression.toString());
    }

    @Test
    public void testNotnullOperatorBool() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title=isnull=false"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title NOTNULL []", expression.toString());
    }

    @Test
    public void testNotnullOperatorInt() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter",
                "title=isnull=0"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title NOTNULL []", expression.toString());
    }
}
