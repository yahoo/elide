/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.security.checks.Check;
import example.Author;
import example.Book;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        public Class<?> lookupBoundClass(Class<?> objClass) {
            // Special handling for mocked Book class which has Entity annotation
            if (objClass.getName().contains("$MockitoMock$")) {
                objClass = objClass.getSuperclass();
            }
            return super.lookupBoundClass(objClass);
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
        assertTrue(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList(1), requestScope);
        assertFalse(fn.test(author));

        // Test contains works
        fn = Operator.IN.contextualize("id", Arrays.asList(1, 2), requestScope);
        assertTrue(fn.test(author));
        fn = Operator.NOT.contextualize("id", Arrays.asList(1, 2), requestScope);
        assertFalse(fn.test(author));

        // Test type
        fn = Operator.IN.contextualize("id", Collections.singletonList("1"), requestScope);
        assertTrue(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList("1"), requestScope);
        assertFalse(fn.test(author));

        // Test not in
        fn = Operator.IN.contextualize("id", Collections.singletonList(3), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList(3), requestScope);
        assertTrue(fn.test(author));

        // Test empty
        fn = Operator.IN.contextualize("id", Collections.emptyList(), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.emptyList(), requestScope);
        assertTrue(fn.test(author));

        // Test null
        author.setId(null);
        fn = Operator.IN.contextualize("id", Collections.singletonList(1), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.NOT.contextualize("id", Collections.singletonList(1), requestScope);
        assertTrue(fn.test(author));
    }

    @Test
    public void isnullAndNotnullTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When name is not null
        fn = Operator.ISNULL.contextualize("name", null, requestScope);
        assertFalse(fn.test(author));
        fn = Operator.NOTNULL.contextualize("name", null, requestScope);
        assertTrue(fn.test(author));

        // When name is null
        author.setName(null);
        fn = Operator.ISNULL.contextualize("name", null, requestScope);
        assertTrue(fn.test(author));
        fn = Operator.NOTNULL.contextualize("name", null, requestScope);
        assertFalse(fn.test(author));
    }

    @Test
    public void isemptyAndNotemptyTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setAwards(Arrays.asList("Booker Prize", "National Book Awards"));
        author.getBooks().add(new Book());

        fn = Operator.ISEMPTY.contextualize("awards", null, requestScope);
        assertFalse(fn.test(author));
        fn = Operator.ISEMPTY.contextualize("books", null, requestScope);
        assertFalse(fn.test(author));
        fn = Operator.NOTEMPTY.contextualize("awards", null, requestScope);
        assertTrue(fn.test(author));
        fn = Operator.NOTEMPTY.contextualize("books", null, requestScope);
        assertTrue(fn.test(author));


        //name is null and books are null
        author.setBooks(null);
        author.setAwards(Arrays.asList());
        fn = Operator.ISEMPTY.contextualize("awards", null, requestScope);
        assertTrue(fn.test(author));
        fn = Operator.ISEMPTY.contextualize("books", null, requestScope);
        assertFalse(fn.test(author));
        fn = Operator.NOTEMPTY.contextualize("awards", null, requestScope);
        assertFalse(fn.test(author));
        fn = Operator.NOTEMPTY.contextualize("books", null, requestScope);
        assertTrue(fn.test(author));

    }

    @Test
    public void prefixAndPostfixAndInfixTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When prefix, infix, postfix are correctly matched
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("Author"), requestScope);
        assertTrue(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("For"), requestScope);
        assertTrue(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("Test"), requestScope);
        assertTrue(fn.test(author));

        // When prefix, infix, postfix are correctly matched if case-insensitive
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("author"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("for"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("test"), requestScope);
        assertFalse(fn.test(author));

        // When prefix, infix, postfix are not matched
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("error"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("error"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("error"), requestScope);
        assertFalse(fn.test(author));

        // When values is null
        author.setName(null);
        fn = Operator.PREFIX.contextualize("name", Collections.singletonList("Author"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.INFIX.contextualize("name", Collections.singletonList("For"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.POSTFIX.contextualize("name", Collections.singletonList("Test"), requestScope);
        assertFalse(fn.test(author));
    }

    @Test
    public void compareOpTests() throws Exception {
        author = new Author();
        author.setId(10L);

        // single value
        fn = Operator.LT.contextualize("id", Collections.singletonList("11"), requestScope);
        assertTrue(fn.test(author));
        fn = Operator.LE.contextualize("id", Collections.singletonList("10"), requestScope);
        assertTrue(fn.test(author));
        fn = Operator.GT.contextualize("id", Collections.singletonList("9"), requestScope);
        assertTrue(fn.test(author));
        fn = Operator.GE.contextualize("id", Collections.singletonList("10"), requestScope);
        assertTrue(fn.test(author));

        // multiple values
        fn = Operator.LT.contextualize("id", Arrays.asList("10", "9"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.LE.contextualize("id", Arrays.asList("9", "8"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.GT.contextualize("id", Arrays.asList("10", "11"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.GE.contextualize("id", Arrays.asList("11", "12"), requestScope);
        assertFalse(fn.test(author));

        // when val is null
        author.setId(null);
        fn = Operator.LT.contextualize("id", Collections.singletonList("10"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.LE.contextualize("id", Collections.singletonList("10"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.GT.contextualize("id", Collections.singletonList("10"), requestScope);
        assertFalse(fn.test(author));
        fn = Operator.GE.contextualize("id", Collections.singletonList("10"), requestScope);
        assertFalse(fn.test(author));
    }

    @Test
    public void testInvalidValueExceptionCases() throws Exception {
        // Test type
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        assertThrows(
                InvalidValueException.class,
                () -> Operator.IN.contextualize("id", Collections.singletonList("a"), requestScope).test(author));

        assertThrows(
                InvalidValueException.class,
                () -> Operator.NOT.contextualize("id", Collections.singletonList("a"), requestScope).test(author));
    }

    @Test
    public void testInvalidPredicateExceptionCases() throws Exception {
        // When num of values != 1
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        assertThrows(
                BadRequestException.class,
                () -> Operator.PREFIX.contextualize("name", Arrays.asList("Author", "Author"), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.INFIX.contextualize("name", Arrays.asList("For", "For"), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.POSTFIX.contextualize("name", Arrays.asList("Test", "Test"), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.PREFIX.contextualize("name", Collections.emptyList(), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.INFIX.contextualize("name", Collections.emptyList(), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.POSTFIX.contextualize("name", Collections.emptyList(), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.LT.contextualize("id", Collections.emptyList(), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.LE.contextualize("id", Collections.emptyList(), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.GT.contextualize("id", Collections.emptyList(), requestScope).test(author));
        assertThrows(
                BadRequestException.class,
                () -> Operator.GE.contextualize("id", Collections.emptyList(), requestScope).test(author));
    }
}
