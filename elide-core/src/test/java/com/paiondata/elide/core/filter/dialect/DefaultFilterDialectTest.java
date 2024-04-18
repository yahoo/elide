/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter.dialect;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
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
 * Tests filter query parameter parsing for the default Elide implementation (1.0 and 2.0).
 */
public class DefaultFilterDialectTest {

    static DefaultFilterDialect dialect;

    @BeforeAll
    public static void init() {
        EntityDictionary dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dialect = new DefaultFilterDialect(dictionary);
    }

    static void add(Map<String, List<String>> params, String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    @Test
    public void testGlobalExpressionParsingWithComplexAttribute() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[author.homeAddress.street1][infix]", "State"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/author", queryParams, NO_VERSION);

        assertEquals("author.homeAddress.street1 INFIX [State]", expression.toString());
    }

    @Test
    public void testGlobalExpressionParsing() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        add(queryParams,
                "filter[author.name][infix]",
                "Hemingway"
        );

        FilterExpression filterExpression = dialect.parseGlobalExpression("/author", queryParams, NO_VERSION);

        assertEquals(
                "(author.books.title IN [foo, bar, baz] AND author.name INFIX [Hemingway])",
                filterExpression.toString()
        );
    }

    @Test
    public void testTypedExpressionParsingWithComplexAttribute() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[author.homeAddress.street1][infix]",
                "State"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals(
                "author.homeAddress.street1 INFIX [State]",
                expressionMap.get("author").toString()
        );
    }

    @Test
    public void testTypedExpressionParsing() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[book.title][in]",
                "foo,bar,baz"
        );

        add(queryParams,
                "filter[book.genre]",
                "scifi"
        );


        add(queryParams,
                "filter[author.name][infix]",
                "Hemingway"
        );

        add(queryParams,
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(2, expressionMap.size());
        assertEquals("(book.title IN [foo, bar, baz] AND book.genre IN [scifi])", expressionMap.get("book").toString());
        assertEquals("(author.name INFIX [Hemingway] AND author.books.title IN [foo, bar, baz])",
                expressionMap.get("author").toString());
    }

    @Test
    public void testInvalidType() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[invalid.title][in]",
                "foo,bar,baz"
        );

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/invalid", queryParams, NO_VERSION));
    }

    @Test
    public void testInvalidAttribute() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[book.invalid][in]",
                "foo,bar,baz"
        );

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));
    }

    @Test
    public void testInvalidTypeQualifier() throws ParseException {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        /* This is now OK for global and for subqueries */
        add(queryParams,
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals("author.books.title IN [foo, bar, baz]", expressionMap.get("author").toString());
    }

    @Test
    public void testBetweenOperator() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[author.books.id][between]",
                "10,20"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals("author.books.id BETWEEN [10, 20]", expressionMap.get("author").toString());
    }

    @Test
    public void testEmptyOperatorException() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[book.authors.name][isempty]",
                ""
        );

        assertThrows(ParseException.class,
                () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));
    }

    @Test
    public void testMemberOfOperator() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams,
                "filter[book.awards][hasnomember]",
                "awards1"
        );

        assertEquals(
                "{book=book.awards HASNOMEMBER [awards1]}",
                dialect.parseTypedExpression("/book", queryParams, NO_VERSION).toString()
        );
    }

    @Test
    public void testMemberOfToManyRelationship() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[book.authors.name][hasmember]",
                "name"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/book", queryParams,
                NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals("book.authors.name HASMEMBER [name]", expressionMap.get("book").toString());
    }

    @Test
    public void testMemberOfOperatorOnNonCollectionAttributeException() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams,
                "filter[book.title][hasmember]",
                "title"
        );

        ParseException e = assertThrows(ParseException.class,
                () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));

        assertEquals("Invalid Path: Last Path Element has to be a collection type", e.getMessage());
    }

    @Test
    public void testMemberOfOperatorOnRelationshipException() throws Exception {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        queryParams.clear();
        add(queryParams,
                "filter[book.authors][hasmember]",
                "1"
        );

        ParseException e = assertThrows(ParseException.class,
                () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));

        assertEquals("Invalid Path: Last Path Element cannot be a collection type", e.getMessage());
    }
}
