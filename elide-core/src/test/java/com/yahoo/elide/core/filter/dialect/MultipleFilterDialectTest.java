/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.filter.expression.FilterExpression;

import com.beust.jcommander.internal.Lists;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests MultipleFilterDialect
 */
public class MultipleFilterDialectTest {

    /**
     * Verify that all dialects are iterated over.
     */
    @Test
    public void testGlobalExpressionParsing() throws Exception {
        JoinFilterDialect dialect1 = mock(JoinFilterDialect.class);
        JoinFilterDialect dialect2 = mock(JoinFilterDialect.class);
        FilterExpression filterExpression = mock(FilterExpression.class);

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Lists.newArrayList(dialect1, dialect2),
                Collections.EMPTY_LIST
        );

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        when(dialect1.parseGlobalExpression("/author", queryParams)).thenThrow(new ParseException(""));
        when(dialect2.parseGlobalExpression("/author", queryParams)).thenReturn(filterExpression);

        FilterExpression returnExpression = dialect.parseGlobalExpression("/author", queryParams);

        verify(dialect1, times(1)).parseGlobalExpression("/author", queryParams);
        verify(dialect2, times(1)).parseGlobalExpression("/author", queryParams);

        Assert.assertEquals(filterExpression, returnExpression);
    }

    /**
     * Verify that all dialects are iterated over.
     */
    @Test
    public void testTypedExpressionParsing() throws Exception {
        SubqueryFilterDialect dialect1 = mock(SubqueryFilterDialect.class);
        SubqueryFilterDialect dialect2 = mock(SubqueryFilterDialect.class);
        Map<String, FilterExpression> expressionMap = Collections.EMPTY_MAP;

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Collections.EMPTY_LIST,
                Lists.newArrayList(dialect1, dialect2)
        );

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        when(dialect1.parseTypedExpression("/author", queryParams)).thenThrow(new ParseException(""));
        when(dialect2.parseTypedExpression("/author", queryParams)).thenReturn(expressionMap);

        Map<String, FilterExpression> returnMap = dialect.parseTypedExpression("/author", queryParams);

        verify(dialect1, times(1)).parseTypedExpression("/author", queryParams);
        verify(dialect2, times(1)).parseTypedExpression("/author", queryParams);

        Assert.assertEquals(expressionMap, returnMap);
    }

    /**
     * Verify that missing dialects throws a ParseException
     */
    @Test(expectedExceptions = ParseException.class)
    public void testMissingTypedDialect() throws Exception {

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST
        );

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        dialect.parseTypedExpression("/author", queryParams);
    }

    /**
     * Verify that missing dialects throws a ParseException
     */
    @Test(expectedExceptions = ParseException.class)
    public void testMissingGlobalDialect() throws Exception {

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST
        );

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        dialect.parseGlobalExpression("/author", queryParams);
    }

    /**
     * Verify that the last error is returned when all dialects fail.
     */
    @Test
    public void testGlobalExpressionParseFailure() throws Exception {
        JoinFilterDialect dialect1 = mock(JoinFilterDialect.class);
        JoinFilterDialect dialect2 = mock(JoinFilterDialect.class);

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Lists.newArrayList(dialect1, dialect2),
                Collections.EMPTY_LIST
        );

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        when(dialect1.parseGlobalExpression("/author", queryParams)).thenThrow(new ParseException("one"));
        when(dialect2.parseGlobalExpression("/author", queryParams)).thenThrow(new ParseException("two"));

        try {
            dialect.parseGlobalExpression("/author", queryParams);
        } catch (ParseException e) {
            Assert.assertEquals(e.getMessage(), "two\none");
        }
    }

    /**
     * Verify that the last error is returned when all dialects fail.
     */
    @Test
    public void testTypedExpressionParseFailure() throws Exception {
        SubqueryFilterDialect dialect1 = mock(SubqueryFilterDialect.class);
        SubqueryFilterDialect dialect2 = mock(SubqueryFilterDialect.class);

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Collections.EMPTY_LIST,
                Lists.newArrayList(dialect1, dialect2)
        );

        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        queryParams.add(
                "filter[author.name][infix]",
                "Hemingway"
        );

        when(dialect1.parseTypedExpression("/author", queryParams)).thenThrow(new ParseException("one"));
        when(dialect2.parseTypedExpression("/author", queryParams)).thenThrow(new ParseException("two"));

        try {
            dialect.parseTypedExpression("/author", queryParams);
        } catch (ParseException e) {
            Assert.assertEquals(e.getMessage(), "two\none");
        }
    }
}
