/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.filter;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.filter.dialect.ParseException;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.PredicateExtractionVisitor;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Predicate test class.
 */
public class FilterPredicateTest {
    private static DefaultFilterDialect strategy;

    @BeforeAll
    static void setup() {
        EntityDictionary entityDictionary = EntityDictionary.builder().build();

        entityDictionary.bindEntity(Book.class);
        entityDictionary.bindEntity(Author.class);

        strategy = new DefaultFilterDialect(entityDictionary);
    }

    static void add(Map<String, List<String>> params, String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    private Map<String, Set<FilterPredicate>> parse(Map<String, List<String>> queryParams) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();

        Map<String, FilterExpression> expressionMap;
        try {
            expressionMap = strategy.parseTypedExpression("/book", queryParams, NO_VERSION);
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
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title]", "abc");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Collections.singletonList("abc"), predicate.getValues());
    }

    @Test
    void testSingleFieldMultipleValues() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testMultipleFieldMultipleValues() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title]", "abc,def");
        add(queryParams, "filter[book.genre]", "def,jkl");

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
                    fail(predicate + " case not covered");
            }
        }
    }

    @Test
    void testSingleFieldWithInOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][in]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithNotOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][not]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.NOT, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithPrefixOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][prefix]", "abc");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.PREFIX, predicate.getOperator());
        assertEquals(Collections.singletonList("abc"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithPostfixOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][postfix]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.POSTFIX, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithInfixOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][infix]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithNotPrefixOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][notprefix]", "abc");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.NOT_PREFIX, predicate.getOperator());
        assertEquals(Collections.singletonList("abc"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithNotPostfixOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][notpostfix]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.NOT_POSTFIX, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testSingleFieldWithNotInfixOperator() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.title][notinfix]", "abc,def");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("title", predicate.getField());
        assertEquals(Operator.NOT_INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("abc", "def"), predicate.getValues());
    }

    @Test
    void testMissingType() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[title]", "abc,def");

        assertThrows(BadRequestException.class, () -> parse(queryParams));
    }

    @Test
    void testIntegerFieldType() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.id]", "1");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("id", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Collections.singletonList(1L), predicate.getValues());
    }

    @Test
    void testComplexAttributeFieldType() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[author.homeAddress.street1]", "foo");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("author"));

        FilterPredicate predicate = predicates.get("author").iterator().next();
        assertEquals("street1", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());

        Path path = predicate.getPath();
        assertEquals(path.getPathElements().get(0).getFieldName(), "homeAddress");
        assertEquals(path.getPathElements().get(1).getFieldName(), "street1");
        assertEquals(Collections.singletonList("foo"), predicate.getValues());
    }

    @Test
    void testMultipleIntegerFieldType() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        add(queryParams, "filter[book.id]", "1,2,3");

        Map<String, Set<FilterPredicate>> predicates = parse(queryParams);
        assertTrue(predicates.containsKey("book"));

        FilterPredicate predicate = predicates.get("book").iterator().next();
        assertEquals("id", predicate.getField());
        assertEquals(Operator.IN, predicate.getOperator());
        assertEquals(Arrays.asList(1L, 2L, 3L), predicate.getValues());
    }
}
