/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import com.yahoo.elide.core.EntityDictionary
import com.yahoo.elide.core.RelationshipType
import com.yahoo.elide.core.exceptions.InvalidPredicateException
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect
import com.yahoo.elide.core.filter.dialect.ParseException
import com.yahoo.elide.core.filter.expression.FilterExpression
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor

import example.Author
import example.Book

import org.testng.Assert
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
/**
 * Predicate test class.
 */
public class FilterPredicateTest {
    private DefaultFilterDialect strategy

    @BeforeSuite
    void setup() {
        EntityDictionary entityDictionary = mock(EntityDictionary.class)
        when(entityDictionary.getJsonAliasFor(String.class)).thenReturn("string")
        when(entityDictionary.getJsonAliasFor(Book.class)).thenReturn("book")
        when(entityDictionary.getJsonAliasFor(Author.class)).thenReturn("author")
        when(entityDictionary.getEntityClass("book")).thenReturn(Book.class)
        when(entityDictionary.getEntityClass("author")).thenReturn(Author.class)
        when(entityDictionary.getParameterizedType(Book.class, "title")).thenReturn(String.class)
        when(entityDictionary.getParameterizedType(Book.class, "genre")).thenReturn(String.class)
        when(entityDictionary.getIdType(Book.class)).thenReturn(Integer.class)
        when(entityDictionary.getRelationshipType(Book.class, "title")).thenReturn(RelationshipType.NONE)
        when(entityDictionary.getRelationshipType(Book.class, "genre")).thenReturn(RelationshipType.NONE)
        when(entityDictionary.getRelationshipType(Book.class, "id")).thenReturn(RelationshipType.NONE)
        strategy = new DefaultFilterDialect(entityDictionary);
    }

    private Map<String, Set<FilterPredicate>> parse(MultivaluedMap<String, String> queryParams) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();

        Map<String, FilterExpression> expressionMap;
        try {
            expressionMap = strategy.parseTypedExpression("/book", queryParams);
        } catch (ParseException e) {
            throw new InvalidPredicateException(e.getMessage());
        }

        Map<String, Set<FilterPredicate>> returnMap = new HashMap<>();
        for (entry in expressionMap) {
            String typeName = entry.key;
            FilterExpression expression = entry.value;
            returnMap[typeName] = returnMap[typeName] ?: new HashSet<FilterPredicate>();
            returnMap[typeName].addAll(expression.accept(visitor));
        }
        return returnMap;
    }

    @Test
    void testSingleFieldSingleValue() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title]", "abc")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "title")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc"])
    }

    @Test
    void testSingleFieldMultipleValues() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "title")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    void testMultipleFieldMultipleValues() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title]", "abc,def")
        queryParams.add("filter[book.genre]", "def,jkl")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        for (FilterPredicate predicate : predicates.get("book")) {
            switch (predicate.getField()) {
                case "title":
                    Assert.assertEquals(predicate.getOperator(), Operator.IN)
                    Assert.assertEquals(predicate.getValues(), ["abc", "def"])
                    break
                case "genre":
                    Assert.assertEquals(predicate.getOperator(), Operator.IN)
                    Assert.assertEquals(predicate.getValues(), ["def", "jkl"])
                    break
                default:
                    Assert.fail(predicate.toString() + " case not covered")
            }
        }
    }

    @Test
    void testSingleFieldWithInOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title][in]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "title")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    void testSingleFieldWithNotOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title][not]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "title")
        Assert.assertEquals(predicate.getOperator(), Operator.NOT)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    void testSingleFieldWithPrefixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title][prefix]", "abc")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "title")
        Assert.assertEquals(predicate.getOperator(), Operator.PREFIX)
        Assert.assertEquals(predicate.getValues(), ["abc"])
    }

    @Test
    void testSingleFieldWithPostfixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title][postfix]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "title")
        Assert.assertEquals(predicate.getOperator(), Operator.POSTFIX)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    void testSingleFieldWithInfixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.title][infix]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "title")
        Assert.assertEquals(predicate.getOperator(), Operator.INFIX)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test(expectedExceptions = [InvalidPredicateException])
    void testMissingType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[title]", "abc,def")

        def predicates = parse(queryParams)
    }

    @Test
    void testIntegerFieldType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.id]", "1")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "id")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), [1])
    }

    @Test
    void testMultipleIntegerFieldType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[book.id]", "1,2,3")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("book"))

        def predicate = predicates.get("book").iterator().next()
        Assert.assertEquals(predicate.getField(), "id")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), [1, 2, 3])
    }
}
