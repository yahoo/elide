/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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

    @Test
    public void testGlobalExpressionParsingWithComplexAttribute() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.homeAddress.street1][infix]", "State"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/author", queryParams, NO_VERSION);

        assertEquals("author.homeAddress.street1 INFIX [State]", expression.toString());
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

        FilterExpression filterExpression = dialect.parseGlobalExpression("/author", queryParams, NO_VERSION);

        assertEquals(
                "(author.books.title IN [foo, bar, baz] AND author.name INFIX [Hemingway])",
                filterExpression.toString()
        );
    }

    @Test
    public void testTypedExpressionParsingWithComplexAttribute() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
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

        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(2, expressionMap.size());
        assertEquals("(book.title IN [foo, bar, baz] AND book.genre IN [scifi])", expressionMap.get("book").toString());
        assertEquals("(author.books.title IN [foo, bar, baz] AND author.name INFIX [Hemingway])",
                expressionMap.get("author").toString());
    }

    @Test
    public void testInvalidType() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[invalid.title][in]",
                "foo,bar,baz"
        );

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/invalid", queryParams, NO_VERSION));
    }

    @Test
    public void testInvalidAttribute() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book.invalid][in]",
                "foo,bar,baz"
        );

        assertThrows(ParseException.class, () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));
    }

    @Test
    public void testInvalidTypeQualifier() throws ParseException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        /* This is now OK for global and for subqueries */
        queryParams.add(
                "filter[author.books.title][in]",
                "foo,bar,baz"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals("author.books.title IN [foo, bar, baz]", expressionMap.get("author").toString());
    }

    @Test
    public void testBetweenOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author.books.id][between]",
                "10,20"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals("author.books.id BETWEEN [10, 20]", expressionMap.get("author").toString());
    }

    @Test
    public void testEmptyOperatorException() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book.authors.name][isempty]",
                ""
        );

        assertThrows(ParseException.class,
                () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));
    }

    @Test
    public void testMemberOfOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add(
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
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
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
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add(
                "filter[book.title][hasmember]",
                "title"
        );

        ParseException e = assertThrows(ParseException.class,
                () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));

        assertEquals("Invalid Path: Last Path Element has to be a collection type", e.getMessage());
    }

    @Test
    public void testMemberOfOperatorOnRelationshipException() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.clear();
        queryParams.add(
                "filter[book.authors][hasmember]",
                "1"
        );

        ParseException e = assertThrows(ParseException.class,
                () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));

        assertEquals("Invalid Path: Last Path Element cannot be a collection type", e.getMessage());
    }
}
