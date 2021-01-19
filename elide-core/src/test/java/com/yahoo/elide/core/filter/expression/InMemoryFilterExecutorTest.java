/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.predicates.FalsePredicate;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.GEPredicate;
import com.yahoo.elide.core.filter.predicates.GTPredicate;
import com.yahoo.elide.core.filter.predicates.InInsensitivePredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.filter.predicates.InfixInsensitivePredicate;
import com.yahoo.elide.core.filter.predicates.InfixPredicate;
import com.yahoo.elide.core.filter.predicates.IsEmptyPredicate;
import com.yahoo.elide.core.filter.predicates.IsNullPredicate;
import com.yahoo.elide.core.filter.predicates.LEPredicate;
import com.yahoo.elide.core.filter.predicates.LTPredicate;
import com.yahoo.elide.core.filter.predicates.NotEmptyPredicate;
import com.yahoo.elide.core.filter.predicates.NotInInsensitivePredicate;
import com.yahoo.elide.core.filter.predicates.NotInPredicate;
import com.yahoo.elide.core.filter.predicates.NotNullPredicate;
import com.yahoo.elide.core.filter.predicates.PostfixInsensitivePredicate;
import com.yahoo.elide.core.filter.predicates.PostfixPredicate;
import com.yahoo.elide.core.filter.predicates.PrefixInsensitivePredicate;
import com.yahoo.elide.core.filter.predicates.PrefixPredicate;
import com.yahoo.elide.core.filter.predicates.TruePredicate;
import com.yahoo.elide.core.type.Type;
import example.Author;
import example.Book;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Tests InMemoryFilterExecutor.
 */
public class InMemoryFilterExecutorTest {
    private Author author;
    private final InMemoryFilterExecutor visitor;
    private FilterExpression expression;
    private Predicate fn;

    private PathElement authorIdElement = new PathElement(Author.class, Long.class, "id");
    private PathElement authorNameElement = new PathElement(Author.class, String.class, "name");
    private PathElement authorBooksElement = new PathElement(Author.class, Book.class, "books");
    private PathElement authorAwardsElement = new PathElement(Author.class, String.class, "awards");
    private List<Object> listNine = Collections.singletonList("9");
    private List<Object> listTen = Collections.singletonList("10");
    private List<Object> listEleven = Collections.singletonList("11");

    public class TestEntityDictionary extends EntityDictionary {

        public TestEntityDictionary(Map checks) {
            super(checks);
        }
        @Override
        public Type<?> lookupBoundClass(Type<?> objClass) {
            // Special handling for mocked Book class which has Entity annotation
            if (objClass.getName().contains("$MockitoMock$")) {
                objClass = objClass.getSuperclass();
            }
            return super.lookupBoundClass(objClass);
        }

    }

    InMemoryFilterExecutorTest() {
        EntityDictionary dictionary = new TestEntityDictionary(new HashMap<>());
        dictionary.bindEntity(Author.class);
        RequestScope requestScope = Mockito.mock(RequestScope.class);
        when(requestScope.getDictionary()).thenReturn(dictionary);
        visitor = new InMemoryFilterExecutor(requestScope);
    }

    @Test
    public void inAndNotInPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // Test exact match
        expression = new InPredicate(authorIdElement, 1L);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotInPredicate(authorIdElement, 1L);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // Test exact match insensitive
        expression = new InInsensitivePredicate(authorNameElement, author.getName().toUpperCase(Locale.ENGLISH));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotInInsensitivePredicate(authorNameElement, author.getName().toUpperCase(Locale.ENGLISH));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // Test contains works
        expression = new InPredicate(authorIdElement, 1, 2);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotInPredicate(authorIdElement, 1, 2);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // Test type
        expression = new InPredicate(authorIdElement, "1");
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotInPredicate(authorIdElement, "1");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // Test not in
        expression = new InPredicate(authorIdElement, 3L);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotInPredicate(authorIdElement, 3L);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        // Test empty
        expression = new InPredicate(authorIdElement, Collections.emptyList());
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotInPredicate(authorIdElement, Collections.emptyList());
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        // Test TRUE/FALSE
        expression = new TruePredicate(authorIdElement);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new FalsePredicate(authorIdElement);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // Test null
        author.setId(null);
        expression = new InPredicate(authorIdElement, 1);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotInPredicate(authorIdElement, 1);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
    }

    @Test
    public void isnullAndNotnullPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When name is not null
        expression = new IsNullPredicate(authorNameElement);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotNullPredicate(authorNameElement);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        // When name is null
        author.setName(null);
        expression = new IsNullPredicate(authorNameElement);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotNullPredicate(authorNameElement);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
    }

    @Test
    public void isemptyAndNotemptyPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        // When name is empty and books are empty
        author.setBooks(new HashSet<>());
        author.setAwards(new HashSet<>());

        expression = new IsEmptyPredicate(authorAwardsElement);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new IsEmptyPredicate(authorBooksElement);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        expression = new NotEmptyPredicate(authorAwardsElement);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotEmptyPredicate(authorBooksElement);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));


        // When name and books are not empty
        author.setAwards(Arrays.asList("Bookery prize"));
        author.getBooks().add(new Book());

        expression = new IsEmptyPredicate(authorAwardsElement);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new IsEmptyPredicate(authorBooksElement);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        expression = new NotEmptyPredicate(authorAwardsElement);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotEmptyPredicate(authorBooksElement);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
    }

    @Test
    public void hasMemberPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        // When name is empty and books are empty
        author.setAwards(new HashSet<>());

        expression = new FilterPredicate(authorAwardsElement, Operator.HASMEMBER, Arrays.asList(""));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new FilterPredicate(authorAwardsElement, Operator.HASNOMEMBER, Arrays.asList(""));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));


        // When name and books are not empty
        author.setAwards(Arrays.asList("Bookery prize"));

        expression = new FilterPredicate(authorAwardsElement, Operator.HASMEMBER, Arrays.asList("Bookery prize"));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new FilterPredicate(authorAwardsElement, Operator.HASNOMEMBER, Arrays.asList("National Book Awards"));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

    }

    @Test
    public void prefixAndPostfixAndInfixPredicateTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        // When prefix, infix, postfix are correctly matched
        expression = new PrefixPredicate(authorNameElement, "Author");
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new InfixPredicate(authorNameElement, "For");
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new PostfixPredicate(authorNameElement, "Test");
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        // When prefix, infix, postfix are correctly matched if case-insensitive
        expression = new PrefixPredicate(authorNameElement, "author");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new InfixPredicate(authorNameElement, "for");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new PostfixPredicate(authorNameElement, "test");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // When prefix, infix, postfix are correctly matched if case-insensitive
        expression = new PrefixInsensitivePredicate(authorNameElement, "author");
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new InfixInsensitivePredicate(authorNameElement, "for");
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new PostfixInsensitivePredicate(authorNameElement, "test");
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));


        // When prefix, infix, postfix are not matched
        expression = new PrefixPredicate(authorNameElement, "error");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new InfixPredicate(authorNameElement, "error");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new PostfixPredicate(authorNameElement, "error");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // When values is null
        author.setName(null);
        expression = new PrefixPredicate(authorNameElement, "Author");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new InfixPredicate(authorNameElement, "For");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new PostfixPredicate(authorNameElement, "Test");
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
    }

    @Test
    public void compareOpPredicateTests() throws Exception {
        author = new Author();
        author.setId(10L);

        expression = new LTPredicate(authorIdElement, listEleven);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new LEPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new GTPredicate(authorIdElement, listNine);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new GEPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new LTPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new LEPredicate(authorIdElement, listNine);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new GTPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new GEPredicate(authorIdElement, listEleven);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        // when val is null
        author.setId(null);
        expression = new LTPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new LEPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new GTPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new GEPredicate(authorIdElement, listTen);
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
    }

    @Test
    public void negativeTests() throws Exception {
        author = new Author();
        author.setId(10L);
        PathElement pathElement = new PathElement(Author.class, Long.class, "id");

        expression = new NotFilterExpression(new LTPredicate(pathElement, listEleven));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotFilterExpression(new LEPredicate(pathElement, listTen));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotFilterExpression(new GTPredicate(pathElement, listNine));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotFilterExpression(new GEPredicate(pathElement, listTen));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
        expression = new NotFilterExpression(new LTPredicate(pathElement, listTen));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotFilterExpression(new LEPredicate(pathElement, listNine));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotFilterExpression(new GTPredicate(pathElement, listTen));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
        expression = new NotFilterExpression(new GEPredicate(pathElement, listEleven));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));
    }

    @Test
    public void andExpressionTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        expression = new AndFilterExpression(
                new InPredicate(authorIdElement, 1L),
                new InPredicate(authorNameElement, "AuthorForTest"));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        expression = new AndFilterExpression(
                new InPredicate(authorIdElement, 0L),
                new InPredicate(authorNameElement, "AuthorForTest"));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        expression = new AndFilterExpression(
                new InPredicate(authorIdElement, 1L),
                new InPredicate(authorNameElement, "Fail"));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));

        expression = new AndFilterExpression(
                new InPredicate(authorIdElement, 0L),
                new InPredicate(authorNameElement, "Fail"));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
    }

    @Test
    public void orExpressionTest() throws Exception {
        author = new Author();
        author.setId(1L);
        author.setName("AuthorForTest");

        expression = new OrFilterExpression(
                new InPredicate(authorIdElement, 1L),
                new InPredicate(authorNameElement, "AuthorForTest"));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new InPredicate(authorIdElement, 0L),
                new InPredicate(authorNameElement, "AuthorForTest"));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new InPredicate(authorIdElement, 1L),
                new InPredicate(authorNameElement, "Fail"));
        fn = expression.accept(visitor);
        assertTrue(fn.test(author));

        expression = new OrFilterExpression(
                new InPredicate(authorIdElement, 0L),
                new InPredicate(authorNameElement, "Fail"));
        fn = expression.accept(visitor);
        assertFalse(fn.test(author));
    }
}
