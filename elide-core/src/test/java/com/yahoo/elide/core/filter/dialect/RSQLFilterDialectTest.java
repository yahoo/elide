/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import example.Author;
import example.Book;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests RSQLFilterDialect
 */
public class RSQLFilterDialectTest {
    private RSQLFilterDialect dialect;

    @BeforeTest
    public void init() {
        EntityDictionary dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dialect = new RSQLFilterDialect(dictionary);
    }

    @Test
    public void testTypedExpressionParsing() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book]",
                "title==*foo*;title!=bar*;(genre=in=(sci-fi,action),publishDate>123)"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams);

        Assert.assertEquals(expressionMap.size(), 1);
        Assert.assertEquals(expressionMap.get("book").toString(),
                "((book.title INFIX_CASE_INSENSITIVE [foo] AND NOT (book.title PREFIX_CASE_INSENSITIVE [bar])) "
                        + "AND (book.genre IN_INSENSITIVE [sci-fi, action] OR book.publishDate GT [123]))"
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

        Assert.assertEquals(expression.toString(),
                "(book.title INFIX_CASE_INSENSITIVE [foo] AND book.authors.name IN_INSENSITIVE [Hemingway])"
        );
    }

    @Test
    public void testEqualOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "book.title IN_INSENSITIVE [Hemingway]"
        );
    }

    @Test
    public void testNotEqualOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title!=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "NOT (book.title IN_INSENSITIVE [Hemingway])"
        );
    }

    @Test
    public void testInOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=in=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "book.title IN_INSENSITIVE [Hemingway]"
        );
    }

    @Test
    public void testOutOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=out=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "NOT (book.title IN_INSENSITIVE [Hemingway])"
        );
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

        Assert.assertEquals(expression.toString(),
                "((((book.publishDate GT [5] OR book.publishDate GE [5]) OR book.publishDate LT [10]) OR book.publishDate LE [10]) "
                        + "AND "
                        + "(((book.publishDate GT [5] OR book.publishDate GE [5]) OR book.publishDate LT [10]) OR book.publishDate LE [10]))"
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

        Assert.assertEquals(expression.toString(),
                "((book.title INFIX_CASE_INSENSITIVE [Hemingway] OR book.title POSTFIX_CASE_INSENSITIVE [Hemingway]) OR book.title PREFIX_CASE_INSENSITIVE [Hemingway])"
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

        Assert.assertEquals(expression.toString(),
                "(book.title IN_INSENSITIVE [foo] AND (book.title IN_INSENSITIVE [bar] AND book.title IN_INSENSITIVE [baz]))"
        );
    }

    @Test
    public void testIsnullOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=true"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "book.title ISNULL []"
        );
    }

    @Test
    public void testIsnullOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=1"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "book.title ISNULL []"
        );
    }

    @Test
    public void testNotnullOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=false"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "book.title NOTNULL []"
        );
    }

    @Test
    public void testNotnullOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=0"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams);

        Assert.assertEquals(expression.toString(),
                "book.title NOTNULL []"
        );
    }
}
