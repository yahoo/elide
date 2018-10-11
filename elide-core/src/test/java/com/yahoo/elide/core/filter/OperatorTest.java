/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import static org.mockito.Mockito.when;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.security.checks.Check;

import example.Author;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class OperatorTest {
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

    private final RequestScope requestScope;
    private Author author;
    private Predicate fn;

    OperatorTest() {
        EntityDictionary dictionary = new TestEntityDictionary(new HashMap<>());
        dictionary.bindEntity(Author.class);
        requestScope = Mockito.mock(RequestScope.class);
        when(requestScope.getDictionary()).thenReturn(dictionary);
    }

    @Test
    public void inAndNotInTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // Test exact match
        fn = Operator.IN.contextualize("id", Collections.singletonList(1), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList(1), requestScope);
        Assert.assertFalse(fn.test(author));

        // Test contains works
        fn = Operator.IN.contextualize("id", Arrays.asList(1, 2), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOT.contextualize("id", Arrays.asList(1, 2), requestScope);
        Assert.assertFalse(fn.test(author));

        // Test type
        fn = Operator.IN.contextualize("id", Collections.singletonList("1"), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList("1"), requestScope);
        Assert.assertFalse(fn.test(author));

        // Test not in
        fn = Operator.IN.contextualize("id", Collections.singletonList(3), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList(3), requestScope);
        Assert.assertTrue(fn.test(author));

        // Test empty
        fn = Operator.IN.contextualize("id", Collections.emptyList(), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.emptyList(), requestScope);
        Assert.assertTrue(fn.test(author));

        // Test null
        author.setId(null);
        fn = Operator.IN.contextualize("id", Collections.singletonList(1), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList(1), requestScope);
        Assert.assertTrue(fn.test(author));
    }

    @Test
    public void isnullAndNotnullTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When name is not null
        fn = Operator.ISNULL.contextualize("name", null, requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.NOTNULL.contextualize("name", null, requestScope);
        Assert.assertTrue(fn.test(author));

        // When name is null
        author.setName(null);
        fn = Operator.ISNULL.contextualize("name", null, requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOTNULL.contextualize("name", null, requestScope);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void prefixAndPostfixAndInfixTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When prefix, infix, postfix are correctly matched
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("Author"), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("For"), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("Test"), requestScope);
        Assert.assertTrue(fn.test(author));

        // When prefix, infix, postfix are correctly matched if case-insensitive
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("author"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("for"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("test"), requestScope);
        Assert.assertFalse(fn.test(author));

        // When prefix, infix, postfix are not matched
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("error"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("error"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("error"), requestScope);
        Assert.assertFalse(fn.test(author));

        // When values is null
        author.setName(null);
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("Author"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("For"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("Test"), requestScope);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void compareOpTests() throws Exception {
        author = new Author();
        author.setId(10L);

        // single value
        fn = Operator.LT.contextualize("id", Collections.singletonList("11"), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.LE.contextualize("id", Collections.singletonList("10"), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.GT.contextualize("id", Collections.singletonList("9"), requestScope);
        Assert.assertTrue(fn.test(author));
        fn = Operator.GE.contextualize("id", Collections.singletonList("10"), requestScope);
        Assert.assertTrue(fn.test(author));

        // multiple values
        fn = Operator.LT.contextualize("id", Arrays.asList("10", "9"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.LE.contextualize("id", Arrays.asList("9", "8"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.GT.contextualize("id", Arrays.asList("10", "11"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.GE.contextualize("id", Arrays.asList("11", "12"), requestScope);
        Assert.assertFalse(fn.test(author));

        // when val is null
        author.setId(null);
        fn = Operator.LT.contextualize("id", Collections.singletonList("10"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.LE.contextualize("id", Collections.singletonList("10"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.GT.contextualize("id", Collections.singletonList("10"), requestScope);
        Assert.assertFalse(fn.test(author));
        fn = Operator.GE.contextualize("id", Collections.singletonList("10"), requestScope);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void testInvalidValueExceptionCases() throws Exception {
        // Test type
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");
        try {
            Operator.IN.contextualize("id", Collections.singletonList("a"), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidValueException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.NOT.contextualize("id", Collections.singletonList("a"), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidValueException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testInvalidPredicateExceptionCases() throws Exception {
        // When num of values != 1
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        try {
            Operator.PREFIX.contextualize("name", Arrays.asList("Author", "Author"), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.INFIX.contextualize("name", Arrays.asList("For", "For"), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.POSTFIX.contextualize("name", Arrays.asList("Test", "Test"), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.PREFIX.contextualize("name", Collections.emptyList(), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.INFIX.contextualize("name", Collections.emptyList(), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.POSTFIX.contextualize("name", Collections.emptyList(), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.LT.contextualize("id", Collections.emptyList(), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.LE.contextualize("id", Collections.emptyList(), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.GT.contextualize("id", Collections.emptyList(), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.GE.contextualize("id", Collections.emptyList(), requestScope).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }
    }
}
