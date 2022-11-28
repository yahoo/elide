/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
                Arrays.asList(dialect1, dialect2),
                Collections.emptyList()
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

        when(dialect1.parseGlobalExpression("/author", queryParams, NO_VERSION)).thenThrow(new ParseException(""));
        when(dialect2.parseGlobalExpression("/author", queryParams, NO_VERSION)).thenReturn(filterExpression);

        FilterExpression returnExpression = dialect.parseGlobalExpression("/author", queryParams, NO_VERSION);

        verify(dialect1, times(1)).parseGlobalExpression("/author", queryParams, NO_VERSION);
        verify(dialect2, times(1)).parseGlobalExpression("/author", queryParams, NO_VERSION);

        assertEquals(returnExpression, filterExpression);
    }

    /**
     * Verify that all dialects are iterated over.
     */
    @Test
    public void testTypedExpressionParsing() throws Exception {
        SubqueryFilterDialect dialect1 = mock(SubqueryFilterDialect.class);
        SubqueryFilterDialect dialect2 = mock(SubqueryFilterDialect.class);
        Map<String, FilterExpression> expressionMap = Collections.emptyMap();

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Collections.emptyList(),
                Arrays.asList(dialect1, dialect2)
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

        when(dialect1.parseTypedExpression("/author", queryParams, NO_VERSION)).thenThrow(new ParseException(""));
        when(dialect2.parseTypedExpression("/author", queryParams, NO_VERSION)).thenReturn(expressionMap);

        Map<String, FilterExpression> returnMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        verify(dialect1, times(1)).parseTypedExpression("/author", queryParams, NO_VERSION);
        verify(dialect2, times(1)).parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(returnMap, expressionMap);
    }

    /**
     * Verify that missing dialects throws a ParseException
     */
    @Test
    public void testMissingTypedDialect() throws Exception {

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Collections.emptyList(),
                Collections.emptyList()
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

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/author", queryParams, NO_VERSION));
    }

    /**
     * Verify that missing dialects throws a ParseException
     */
    @Test
    public void testMissingGlobalDialect() throws Exception {

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Collections.emptyList(),
                Collections.emptyList()
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

        assertThrows(ParseException.class, () -> dialect.parseGlobalExpression("/author", queryParams, NO_VERSION));
    }

    /**
     * Verify that the last error is returned when all dialects fail.
     */
    @Test
    public void testGlobalExpressionParseFailure() throws Exception {
        JoinFilterDialect dialect1 = mock(JoinFilterDialect.class);
        JoinFilterDialect dialect2 = mock(JoinFilterDialect.class);

        MultipleFilterDialect dialect = new MultipleFilterDialect(
                Arrays.asList(dialect1, dialect2),
                Collections.emptyList()
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

        when(dialect1.parseGlobalExpression("/author", queryParams, NO_VERSION)).thenThrow(new ParseException("one"));
        when(dialect2.parseGlobalExpression("/author", queryParams, NO_VERSION)).thenThrow(new ParseException("two"));

        try {
            dialect.parseGlobalExpression("/author", queryParams, NO_VERSION);
        } catch (ParseException e) {
            assertEquals("two\none", e.getMessage());
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
                Collections.emptyList(),
                Arrays.asList(dialect1, dialect2)
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

        when(dialect1.parseTypedExpression("/author", queryParams, NO_VERSION)).thenThrow(new ParseException("one"));
        when(dialect2.parseTypedExpression("/author", queryParams, NO_VERSION)).thenThrow(new ParseException("two"));

        try {
            dialect.parseTypedExpression("/author", queryParams, NO_VERSION);
        } catch (ParseException e) {
            assertEquals("two\none", e.getMessage());
        }
    }
}
