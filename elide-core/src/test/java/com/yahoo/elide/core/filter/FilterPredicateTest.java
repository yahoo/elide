/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;

import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;


/**
 * Predicate test class.
 */
public class FilterPredicateTest {
    private static DefaultFilterDialect strategy;

    @BeforeAll
    static void setup() {
        EntityDictionary entityDictionary = mock(EntityDictionary.class);
        when(entityDictionary.getJsonAliasFor(String.class)).thenReturn("string");
        when(entityDictionary.getJsonAliasFor(Book.class)).thenReturn("book");
        when(entityDictionary.getJsonAliasFor(Author.class)).thenReturn("author");

        doReturn(Book.class).when(entityDictionary).getEntityClass("book");
        doReturn(Author.class).when(entityDictionary).getEntityClass("author");
        doReturn(String.class).when(entityDictionary).getParameterizedType(Book.class, "title");
        doReturn(String.class).when(entityDictionary).getParameterizedType(Book.class, "genre");
        doReturn(Integer.class).when(entityDictionary).getIdType(Book.class);

        when(entityDictionary.getRelationshipType(Book.class, "title")).thenReturn(RelationshipType.NONE);
        when(entityDictionary.getRelationshipType(Book.class, "genre")).thenReturn(RelationshipType.NONE);
        when(entityDictionary.getRelationshipType(Book.class, "id")).thenReturn(RelationshipType.NONE);

        strategy = new DefaultFilterDialect(entityDictionary);
    }

    private Map<String, Set<FilterPredicate>> parse(MultivaluedMap<String, String> queryParams) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();

        Map<String, FilterExpression> expressionMap;
        try {
            expressionMap = strategy.parseTypedExpression("/book", queryParams);
        } catch (ParseException e) {
            throw new BadRequestException(e.getMessage());
        }

        Map<String, Set<FilterPredicate>> returnMap = new HashMap<>();
        for (Map.Entry<String, FilterExpression> entry : expressionMap.entrySet()) {
            String typeName = entry.getKey();
            FilterExpression expression = entry.getValue();

            if (!returnMap.containsKey(typeName)) {
                returnMap.put(typeName, new HashSet<>());
            }

            returnMap.get(typeName).addAll(expression.accept(visitor));
        }
        return returnMap;
    }

    @Test
    void testSingleFieldSingleValue() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title]", "abc");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Collections.singletonList("abc"), predicate.getValues());
    }

    @Test
    void testSingleFieldMultipleValues() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testMultipleFieldMultipleValues() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title]", "abc,def");
        queryParams.add("filter[book.genre]", "def,jkl");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        for (FilterPredicate predicate : predicates.get("book")) {
            switch (predicate.getField()) {
                case "title":
                    assertEquals(Operator.IN, predicate.getOperator());
                    assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
                    break;
                case "genre":
                    assertEquals(Operator.IN, predicate.getOperator());
                    assertEquals(Arrays.asList("def", "jkl"), predicate.getValues());
                    break;
                default:
                    fail(predicate.toString() + " case not covered");
            }
        }
    }

    @Test
    void testSingleFieldWithInOperator() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title][in]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithNotOperator() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title][not]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.NOT, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithPrefixOperator() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title][prefix]", "abc");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.PREFIX, predicate.getOperator());
        assertEquals(Collections.singletonList("abc"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithPostfixOperator() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title][postfix]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.POSTFIX, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithInfixOperator() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.title][infix]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testMissingType() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[title]", "abc,def");

        assertThrows(BadRequestException.class, () -> parse(queryParams));
    }

    @Test
    void testIntegerFieldType() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.id]", "1");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("id", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Collections.singletonList(1), predicate.getValues());
    }

    @Test
    void testMultipleIntegerFieldType() {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("filter[book.id]", "1,2,3");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("id", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Arrays.asList(1, 2, 3), predicate.getValues());
    }
}
