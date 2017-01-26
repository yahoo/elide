/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter
import com.yahoo.elide.core.EntityDictionary
import com.yahoo.elide.core.exceptions.InvalidPredicateException
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect
import com.yahoo.elide.core.filter.dialect.ParseException
import example.Author
import example.Book
import org.testng.Assert
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
/**
 * Predicate test class.
 */
public class FilterPredicateTest {
    private DefaultFilterDialect strategy

    @BeforeSuite
    public void setup() {
        EntityDictionary entityDictionary = mock(EntityDictionary.class)
        when(entityDictionary.getJsonAliasFor(String.class)).thenReturn("string")
        when(entityDictionary.getJsonAliasFor(Book.class)).thenReturn("type1")
        when(entityDictionary.getJsonAliasFor(Author.class)).thenReturn("type2")
        when(entityDictionary.getEntityClass("type1")).thenReturn(Book.class)
        when(entityDictionary.getEntityClass("type2")).thenReturn(Author.class)
        when(entityDictionary.getParameterizedType(Book.class, "field1")).thenReturn(String.class)
        when(entityDictionary.getParameterizedType(Book.class, "field2")).thenReturn(String.class)
        when(entityDictionary.getParameterizedType(Book.class, "intField")).thenReturn(Integer.class)
        strategy = new DefaultFilterDialect(entityDictionary);
    }

    private Map<String, Set<FilterPredicate>> parse(MultivaluedMap<String, String> queryParams) {
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();

        def expressionMap;
        try {
            expressionMap = strategy.parseTypedExpression("/type1", queryParams);
        } catch (ParseException e) {
            throw new InvalidPredicateException(e.getMessage());
        }

        def returnMap = new HashMap<String, Set<FilterPredicate>>();
        for (entry in expressionMap) {
            def typeName = entry.key;
            def expression = entry.value;
            if (!returnMap.containsKey(typeName)) {
                returnMap[typeName] = new HashSet<FilterPredicate>();
            }
            returnMap[typeName].addAll(expression.accept(visitor));
        }
        return returnMap;
    }

    @Test
    public void testSingleFieldSingleValue() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1]", "abc")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc"])
    }

    @Test
    public void testSingleFieldMultipleValues() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testMultipleFieldMultipleValues() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1]", "abc,def")
        queryParams.add("filter[type1.field2]", "def,jkl")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        for (FilterPredicate predicate : predicates.get("type1")) {
            switch (predicate.getField()) {
                case "field1":
                    Assert.assertEquals(predicate.getOperator(), Operator.IN)
                    Assert.assertEquals(predicate.getValues(), ["abc", "def"])
                    break
                case "field2":
                    Assert.assertEquals(predicate.getOperator(), Operator.IN)
                    Assert.assertEquals(predicate.getValues(), ["def", "jkl"])
                    break
                default:
                    Assert.fail(predicate.toString() + " case not covered")
            }
        }
    }

    @Test
    public void testSingleFieldWithInOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][in]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testSingleFieldWithNotOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][not]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.NOT)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testSingleFieldWithPrefixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][prefix]", "abc")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.PREFIX)
        Assert.assertEquals(predicate.getValues(), ["abc"])
    }

    @Test
    public void testSingleFieldWithPostfixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][postfix]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.POSTFIX)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testSingleFieldWithInfixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][infix]", "abc,def")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.INFIX)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test(expectedExceptions = [InvalidPredicateException])
    public void testMissingType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[field1]", "abc,def")

        def predicates = parse(queryParams)
    }

    @Test
    public void testIntegerFieldType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.intField]", "1")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "intField")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), [1])
    }

    @Test
    public void testMultipleIntegerFieldType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.intField]", "1,2,3")

        def predicates = parse(queryParams)
        Assert.assertTrue(predicates.containsKey("type1"))

        def predicate = predicates.get("type1").iterator().next()
        Assert.assertEquals(predicate.getField(), "intField")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), [1, 2, 3])
    }
}
