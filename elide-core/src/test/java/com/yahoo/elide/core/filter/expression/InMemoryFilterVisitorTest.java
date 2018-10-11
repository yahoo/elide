/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import static org.mockito.Mockito.when;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.security.checks.Check;

import example.Author;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Tests InMemoryFilterVisitor
 */
public class InMemoryFilterVisitorTest {
    private Author author;
    private final InMemoryFilterVisitor visitor;
    private FilterExpression expression;
    private Predicate fn;

    private PathElement authorIdElement = new PathElement(Author.class, Long.class, "id");
    private PathElement authorNameElement = new PathElement(Author.class, String.class, "name");
    private List<Object> listNine = Collections.singletonList("9");
    private List<Object> listTen = Collections.singletonList("10");
    private List<Object> listEleven = Collections.singletonList("11");

    public class TestEntityDictionary extends EntityDictionary {

        public TestEntityDictionary(Map<String, Class<? extends Check>> checks) {
            super(checks);
        }
        @Override
        public Class<?> lookupEntityClass(Class<?> objClass) {
            // Special handling for mocked Book class which has Entity annotation
            if (objClass.getName().contains("$MockitoMock$")) {
                objClass = objClass.getSuperclass();
            }
            return super.lookupEntityClass(objClass);
        }

    }

    InMemoryFilterVisitorTest() {
        EntityDictionary dictionary = new TestEntityDictionary(new HashMap<>());
        dictionary.bindEntity(Author.class);
        RequestScope requestScope = Mockito.mock(RequestScope.class);
        when(requestScope.getDictionary()).thenReturn(dictionary);
        visitor = new InMemoryFilterVisitor(requestScope);
    }

    @Test
    public void inAndNotInPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // Test exact match
        expression = new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(1L));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.NOT, Collections.singletonList(1L));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test contains works
        expression = new FilterPredicate(authorIdElement, Operator.IN, Arrays.asList(1, 2));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.NOT, Arrays.asList(1, 2));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test type
        expression = new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList("1"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.NOT, Collections.singletonList("1"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test not in
        expression = new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(3L));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.NOT, Collections.singletonList(3L));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // Test empty
        expression = new FilterPredicate(authorIdElement, Operator.IN, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.NOT, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // Test TRUE/FALSE
        expression = new FilterPredicate(authorIdElement, Operator.TRUE, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.FALSE, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test null
        author.setId(null);
        expression = new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(1));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.NOT, Collections.singletonList(1));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
    }

    @Test
    public void isnullAndNotnullPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When name is not null
        expression = new FilterPredicate(authorNameElement, Operator.ISNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.NOTNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // When name is null
        author.setName(null);
        expression = new FilterPredicate(authorNameElement, Operator.ISNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.NOTNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void prefixAndPostfixAndInfixPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When prefix, infix, postfix are correctly matched
        expression = new FilterPredicate(authorNameElement, Operator.PREFIX, Collections.singletonList("Author"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.INFIX, Collections.singletonList("For"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.POSTFIX, Collections.singletonList("Test"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // When prefix, infix, postfix are correctly matched if case-insensitive
        expression = new FilterPredicate(authorNameElement, Operator.PREFIX, Collections.singletonList("author"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.INFIX, Collections.singletonList("for"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.POSTFIX, Collections.singletonList("test"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // When prefix, infix, postfix are not matched
        expression = new FilterPredicate(authorNameElement, Operator.PREFIX, Collections.singletonList("error"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.INFIX, Collections.singletonList("error"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.POSTFIX, Collections.singletonList("error"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // When values is null
        author.setName(null);
        expression = new FilterPredicate(authorNameElement, Operator.PREFIX, Collections.singletonList("Author"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.INFIX, Collections.singletonList("For"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorNameElement, Operator.POSTFIX, Collections.singletonList("Test"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void compareOpPredicateTests() throws Exception {
        author = new Author();
        author.setId(10L);

        expression = new FilterPredicate(authorIdElement, Operator.LT, listEleven);
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.LE, listTen);
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.GT, listNine);
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.GE, listTen);
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.LT, listTen);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.LE, listNine);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.GT, listTen);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.GE, listEleven);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // when val is null
        author.setId(null);
        expression = new FilterPredicate(authorIdElement, Operator.LT, listTen);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.LE, listTen);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.GT, listTen);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(authorIdElement, Operator.GE, listTen);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void negativeTests() throws Exception {
        author = new Author();
        author.setId(10L);
        PathElement pathElement = new PathElement(Author.class, Long.class, "id");

        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.LT, listEleven));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.LE, listTen));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.GT, listNine));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.GE, listTen));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.LT, listTen));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.LE, listNine));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.GT, listTen));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(pathElement, Operator.GE, listEleven));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
    }

    @Test
    public void andExpressionTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        expression = new AndFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new AndFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        expression = new AndFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        expression = new AndFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void orExpressionTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        expression = new OrFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new FilterPredicate(authorIdElement, Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(authorNameElement, Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }
}
