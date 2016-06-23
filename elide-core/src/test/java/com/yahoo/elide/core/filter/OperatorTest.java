/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.security.checks.Check;

import org.testng.Assert;
import org.testng.annotations.Test;

import example.Author;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private final EntityDictionary dictionary;
    private Author author;
    private java.util.function.Predicate fn;

    OperatorTest() {
        dictionary = new TestEntityDictionary(new HashMap<>());
        dictionary.bindEntity(Author.class);
    }

    @Test
    public void inAndNotInTest() throws Exception {
        author = new Author();
        author.setId(1);
        author.setName("AuthorForTest");

        // Test exact match
        fn = Operator.IN.getFilterFunction("id", Arrays.asList(1), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOT.getFilterFunction("id", Arrays.asList(1), dictionary);
        Assert.assertFalse(fn.test(author));

        // Test contains works
        fn = Operator.IN.getFilterFunction("id", Arrays.asList(1, 2), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOT.getFilterFunction("id", Arrays.asList(1, 2), dictionary);
        Assert.assertFalse(fn.test(author));

        // Test type
        fn = Operator.IN.getFilterFunction("id", Arrays.asList("1"), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOT.getFilterFunction("id", Arrays.asList("1"), dictionary);
        Assert.assertFalse(fn.test(author));

        // Test not in
        fn = Operator.IN.getFilterFunction("id", Arrays.asList(3), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.NOT.getFilterFunction("id", Arrays.asList(3), dictionary);
        Assert.assertTrue(fn.test(author));

        // Test empty
        fn = Operator.IN.getFilterFunction("id", Collections.emptyList(), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.NOT.getFilterFunction("id", Collections.emptyList(), dictionary);
        Assert.assertTrue(fn.test(author));
    }

    @Test
    public void isnullAndNotnullTest() throws Exception {
        author = new Author();
        author.setId(1);
        author.setName("AuthorForTest");

        // When name is not null
        fn = Operator.ISNULL.getFilterFunction("name", null, dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.NOTNULL.getFilterFunction("name", null, dictionary);
        Assert.assertTrue(fn.test(author));

        // When name is null
        author.setName(null);
        fn = Operator.ISNULL.getFilterFunction("name", null, dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.NOTNULL.getFilterFunction("name", null, dictionary);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void prefixAndPostfixAndInfixTest() throws Exception {
        author = new Author();
        author.setId(1);
        author.setName("AuthorForTest");

        // When prefix, infix, postfix are correctly matched
        fn = Operator.PREFIX.getFilterFunction("name", Arrays.asList("Author"), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.INFIX.getFilterFunction("name", Arrays.asList("For"), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.POSTFIX.getFilterFunction("name", Arrays.asList("Test"), dictionary);
        Assert.assertTrue(fn.test(author));

        // When prefix, infix, postfix are correctly matched if case-insensitive
        fn = Operator.PREFIX.getFilterFunction("name", Arrays.asList("author"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.INFIX.getFilterFunction("name", Arrays.asList("for"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.POSTFIX.getFilterFunction("name", Arrays.asList("test"), dictionary);
        Assert.assertFalse(fn.test(author));

        // When prefix, infix, postfix are not matched
        fn = Operator.PREFIX.getFilterFunction("name", Arrays.asList("error"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.INFIX.getFilterFunction("name", Arrays.asList("error"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.POSTFIX.getFilterFunction("name", Arrays.asList("error"), dictionary);
        Assert.assertFalse(fn.test(author));

        // When values is null
        author.setName(null);
        fn = Operator.PREFIX.getFilterFunction("name", Arrays.asList("Author"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.INFIX.getFilterFunction("name", Arrays.asList("For"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.POSTFIX.getFilterFunction("name", Arrays.asList("Test"), dictionary);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void compareOpTests() throws Exception {
        author = new Author();
        author.setId(10);

        fn = Operator.LT.getFilterFunction("id", Arrays.asList("11"), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.LE.getFilterFunction("id", Arrays.asList("10"), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.GT.getFilterFunction("id", Arrays.asList("9"), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.GE.getFilterFunction("id", Arrays.asList("10"), dictionary);
        Assert.assertTrue(fn.test(author));
        fn = Operator.LT.getFilterFunction("id", Arrays.asList("10"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.LE.getFilterFunction("id", Arrays.asList("9"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.GT.getFilterFunction("id", Arrays.asList("10"), dictionary);
        Assert.assertFalse(fn.test(author));
        fn = Operator.GE.getFilterFunction("id", Arrays.asList("11"), dictionary);
        Assert.assertFalse(fn.test(author));
    }

    @Test
    public void testInvalidValueExceptionCases() throws Exception {
        // Test type
        author = new Author();
        author.setId(1);
        author.setName("AuthorForTest");
        try {
            Operator.IN.getFilterFunction("id", Arrays.asList("a"), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidValueException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.NOT.getFilterFunction("id", Arrays.asList("a"), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidValueException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testInvalidPredicateExceptionCases() throws Exception {
        // When num of values != 1
        author = new Author();
        author.setId(1);
        author.setName("AuthorForTest");

        try {
            Operator.PREFIX.getFilterFunction("name", Arrays.asList("Author", "Author"), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.INFIX.getFilterFunction("name", Arrays.asList("For", "For"), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.POSTFIX.getFilterFunction("name", Arrays.asList("Test", "Test"), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.PREFIX.getFilterFunction("name", Collections.emptyList(), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.INFIX.getFilterFunction("name", Collections.emptyList(), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.POSTFIX.getFilterFunction("name", Collections.emptyList(), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.LT.getFilterFunction("id", Arrays.asList(10, 10), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.LE.getFilterFunction("id", Arrays.asList(10, 10), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.GT.getFilterFunction("id", Arrays.asList(10, 10), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.GE.getFilterFunction("id", Arrays.asList(10, 10), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.LT.getFilterFunction("id", Collections.emptyList(), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.LE.getFilterFunction("id", Collections.emptyList(), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.GT.getFilterFunction("id", Collections.emptyList(), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }

        try {
            Operator.GE.getFilterFunction("id", Collections.emptyList(), dictionary).test(author);
            Assert.assertTrue(false);
        } catch (InvalidPredicateException e) {
            Assert.assertTrue(true);
        }
    }
}
