/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter

import com.yahoo.elide.core.EntityDictionary
import com.yahoo.elide.core.exceptions.InvalidPredicateException
import org.testng.Assert
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import javax.ws.rs.core.MultivaluedHashMap

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Predicate test class.
 */
public class PredicateTest {
    private EntityDictionary entityDictionary

    @BeforeSuite
    public void setup() {
        entityDictionary = mock(EntityDictionary.class)
        when(entityDictionary.getBinding(String.class)).thenReturn("string")
        when(entityDictionary.getBinding("type1")).thenReturn(String.class)
        when(entityDictionary.getBinding("type2")).thenReturn(String.class)
        when(entityDictionary.getParameterizedType(String.class, "field1")).thenReturn(String.class)
        when(entityDictionary.getParameterizedType(String.class, "field2")).thenReturn(String.class)
        when(entityDictionary.getParameterizedType(String.class, "intField")).thenReturn(Integer.class)
    }

    @Test
    public void testSingleFieldSingleValue() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1]", "abc")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc"])
    }

    @Test
    public void testSingleFieldMultipleValues() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1]", "abc,def")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testMultipleFieldMultipleValues() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1]", "abc,def")
        queryParams.add("filter[type1.field2]", "def,jkl")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 2)

        for (Predicate predicate : predicateSet) {
            Assert.assertEquals(predicate.getType(), "type1")
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

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testSingleFieldWithNotOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][not]", "abc,def")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.NOT)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testSingleFieldWithPrefixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][prefix]", "abc")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.PREFIX)
        Assert.assertEquals(predicate.getValues(), ["abc"])
    }

    @Test
    public void testSingleFieldWithPostfixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][postfix]", "abc,def")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.POSTFIX)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test
    public void testSingleFieldWithInfixOperator() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.field1][infix]", "abc,def")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "field1")
        Assert.assertEquals(predicate.getOperator(), Operator.INFIX)
        Assert.assertEquals(predicate.getValues(), ["abc", "def"])
    }

    @Test(expectedExceptions = [InvalidPredicateException])
    public void testMissingType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[field1]", "abc,def")

        Predicate.parseQueryParams(entityDictionary, queryParams)
    }

    @Test
    public void testIntegerFieldType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.intField]", "1")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "intField")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), [1])
    }

    @Test
    public void testMultipleIntegerFieldType() {
        def queryParams = new MultivaluedHashMap<>()
        queryParams.add("filter[type1.intField]", "1,2,3")

        def predicateSet = Predicate.parseQueryParams(entityDictionary, queryParams)
        Assert.assertEquals(predicateSet.size(), 1)

        def predicate = predicateSet.iterator().next()
        Assert.assertEquals(predicate.getType(), "type1")
        Assert.assertEquals(predicate.getField(), "intField")
        Assert.assertEquals(predicate.getOperator(), Operator.IN)
        Assert.assertEquals(predicate.getValues(), [1, 2, 3])
    }
}
