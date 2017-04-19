/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.books.Author;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.mockito.Mockito.when;

/**
 * Tests InMemoryFilterVisitor
 */
public class InMemoryFilterVisitorTest {

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

    private final EntityDictionary dictionary;
    private Author author;
    private final InMemoryFilterVisitor visitor;
    private FilterExpression expression;
    private Predicate fn;

    InMemoryFilterVisitorTest() {
        dictionary = new TestEntityDictionary(new HashMap<>());
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
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(1L));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.NOT, Collections.singletonList(1L));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test contains works
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Arrays.asList(1, 2));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.NOT, Arrays.asList(1, 2));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test type
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList("1"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.NOT, Collections.singletonList("1"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test not in
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(3L));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.NOT, Collections.singletonList(3L));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // Test empty
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.NOT, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // Test TRUE/FALSE
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.TRUE);
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.FALSE);
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // Test null
        author.setId(null);
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Arrays.asList(1));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.NOT, Arrays.asList(1));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
    }

    @Test
    public void isnullAndNotnullPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When name is not null
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.ISNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.NOTNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // When name is null
        author.setName(null);
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.ISNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.NOTNULL, Collections.emptyList());
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void prefixAndPostfixAndInfixPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When prefix, infix, postfix are correctly matched
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.PREFIX, Arrays.asList("Author"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.INFIX, Arrays.asList("For"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.POSTFIX, Arrays.asList("Test"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        // When prefix, infix, postfix are correctly matched if case-insensitive
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.PREFIX, Arrays.asList("author"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.INFIX, Arrays.asList("for"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.POSTFIX, Arrays.asList("test"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // When prefix, infix, postfix are not matched
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.PREFIX, Arrays.asList("error"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.INFIX, Arrays.asList("error"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.POSTFIX, Arrays.asList("error"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // When values is null
        author.setName(null);
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.PREFIX, Arrays.asList("Author"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.INFIX, Arrays.asList("For"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.POSTFIX, Arrays.asList("Test"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void compareOpPredicateTests() throws Exception {
        author = new Author();
        author.setId(10L);

        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LT, Arrays.asList("11"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LE, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GT, Arrays.asList("9"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GE, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LT, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LE, Arrays.asList("9"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GT, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GE, Arrays.asList("11"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        // when val is null
        author.setId(null);
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LT, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LE, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GT, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GE, Arrays.asList("10"));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void negativeTests() throws Exception {
        author = new Author();
        author.setId(10L);

        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LT, Arrays.asList("11")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LE, Arrays.asList("10")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GT, Arrays.asList("9")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GE, Arrays.asList("10")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LT, Arrays.asList("10")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.LE, Arrays.asList("9")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GT, Arrays.asList("10")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
        expression = new NotFilterExpression(new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.GE, Arrays.asList("11")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));
    }

    @Test
    public void andExpressionTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        expression = new AndFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new AndFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        expression = new AndFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));

        expression = new AndFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void orExpressionTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        expression = new OrFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("AuthorForTest")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(1L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", Long.class, "id"), Operator.IN, Collections.singletonList(0L)),
                new FilterPredicate(new FilterPredicate.PathElement(Author.class, "author", String.class, "name"), Operator.IN, Collections.singletonList("Fail")));
        fn = expression.accept(visitor);
        Assert.assertFalse(fn.test(author));
    }
}
