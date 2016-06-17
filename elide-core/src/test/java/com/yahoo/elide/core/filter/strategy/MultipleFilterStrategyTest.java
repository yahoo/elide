/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.strategy;

import com.beust.jcommander.internal.Lists;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests MultipleFilterStrategy
 */
public class MultipleFilterStrategyTest {

    /**
     * Verify that all strategies are iterated over.
     */
    @Test
    public void testGlobalExpressionParsing() throws Exception {
        JoinFilterStrategy strategy1 = mock(JoinFilterStrategy.class);
        JoinFilterStrategy strategy2 = mock(JoinFilterStrategy.class);
        FilterExpression filterExpression = mock(FilterExpression.class);

        MultipleFilterStrategy strategy = new MultipleFilterStrategy(
                Lists.newArrayList(strategy1, strategy2),
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

        when(strategy1.parseGlobalExpression("/author", queryParams)).thenThrow(new ParseException(""));
        when(strategy2.parseGlobalExpression("/author", queryParams)).thenReturn(filterExpression);

        FilterExpression returnExpression = strategy.parseGlobalExpression("/author", queryParams);

        verify(strategy1, times(1)).parseGlobalExpression("/author", queryParams);
        verify(strategy2, times(1)).parseGlobalExpression("/author", queryParams);

        Assert.assertEquals(filterExpression, returnExpression);
    }

    /**
     * Verify that all strategies are iterated over.
     */
    @Test
    public void testTypedExpressionParsing() throws Exception {
        SubqueryFilterStrategy strategy1 = mock(SubqueryFilterStrategy.class);
        SubqueryFilterStrategy strategy2 = mock(SubqueryFilterStrategy.class);
        Map<String, FilterExpression> expressionMap = Collections.EMPTY_MAP;

        MultipleFilterStrategy strategy = new MultipleFilterStrategy(
                Collections.EMPTY_LIST,
                Lists.newArrayList(strategy1, strategy2)
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

        when(strategy1.parseTypedExpression("/author", queryParams)).thenThrow(new ParseException(""));
        when(strategy2.parseTypedExpression("/author", queryParams)).thenReturn(expressionMap);

        Map<String, FilterExpression> returnMap = strategy.parseTypedExpression("/author", queryParams);

        verify(strategy1, times(1)).parseTypedExpression("/author", queryParams);
        verify(strategy2, times(1)).parseTypedExpression("/author", queryParams);

        Assert.assertEquals(expressionMap, returnMap);
    }

    /**
     * Verify that missing strategies throws a ParseException
     */
    @Test(expectedExceptions = ParseException.class)
    public void testMissingTypedStrategy() throws Exception {

        MultipleFilterStrategy strategy = new MultipleFilterStrategy(
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

        strategy.parseTypedExpression("/author", queryParams);
    }

    /**
     * Verify that missing strategies throws a ParseException
     */
    @Test(expectedExceptions = ParseException.class)
    public void testMissingGlobalStrategy() throws Exception {

        MultipleFilterStrategy strategy = new MultipleFilterStrategy(
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

        strategy.parseGlobalExpression("/author", queryParams);
    }

    /**
     * Verify that the last error is returned when all strategies fail.
     */
    @Test
    public void testGlobalExpressionParseFailure() throws Exception {
        JoinFilterStrategy strategy1 = mock(JoinFilterStrategy.class);
        JoinFilterStrategy strategy2 = mock(JoinFilterStrategy.class);

        MultipleFilterStrategy strategy = new MultipleFilterStrategy(
                Lists.newArrayList(strategy1, strategy2),
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

        when(strategy1.parseGlobalExpression("/author", queryParams)).thenThrow(new ParseException("one"));
        when(strategy2.parseGlobalExpression("/author", queryParams)).thenThrow(new ParseException("two"));

        try {
            strategy.parseGlobalExpression("/author", queryParams);
        } catch (ParseException e) {
            Assert.assertEquals(e.getMessage(), "two\none");
        }
    }

    /**
     * Verify that the last error is returned when all strategies fail.
     */
    @Test
    public void testTypedExpressionParseFailure() throws Exception {
        SubqueryFilterStrategy strategy1 = mock(SubqueryFilterStrategy.class);
        SubqueryFilterStrategy strategy2 = mock(SubqueryFilterStrategy.class);

        MultipleFilterStrategy strategy = new MultipleFilterStrategy(
                Collections.EMPTY_LIST,
                Lists.newArrayList(strategy1, strategy2)
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

        when(strategy1.parseTypedExpression("/author", queryParams)).thenThrow(new ParseException("one"));
        when(strategy2.parseTypedExpression("/author", queryParams)).thenThrow(new ParseException("two"));

        try {
            strategy.parseTypedExpression("/author", queryParams);
        } catch (ParseException e) {
            Assert.assertEquals(e.getMessage(), "two\none");
        }
    }
}
