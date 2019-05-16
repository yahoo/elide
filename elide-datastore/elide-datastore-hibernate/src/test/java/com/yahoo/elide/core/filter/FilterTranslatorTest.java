/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests Building a HQL Filter.
 */
public class FilterTranslatorTest {

    class Book {
        String name;
        String genre;
        Author author;
    }

    class Author {
        String name;
    }

    @Test
    public void testHQLQueryVisitor() throws Exception {
        List<Path.PathElement> p1Path = Arrays.asList(
                new Path.PathElement(Book.class, Author.class, "authors"),
                new Path.PathElement(Author.class, String.class, "name")
        );
        FilterPredicate p1 = new InPredicate(new Path(p1Path), "foo", "bar");

        List<Path.PathElement> p2Path = Arrays.asList(
                new Path.PathElement(Book.class, String.class, "name")
        );
        FilterPredicate p2 = new InPredicate(new Path(p2Path), "blah");

        List<Path.PathElement> p3Path = Arrays.asList(
                new Path.PathElement(Book.class, String.class, "genre")
        );
        FilterPredicate p3 = new InPredicate(new Path(p3Path), "scifi");

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and = new AndFilterExpression(or, p1);
        NotFilterExpression not = new NotFilterExpression(and);

        FilterTranslator filterOp = new FilterTranslator();
        String query = filterOp.apply(not, false);

        String p1Params = p1.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String p2Params = p2.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String p3Params = p3.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String expected = "WHERE NOT (((name IN (" + p2Params + ") OR genre IN (" + p3Params + ")) "
                + "AND authors.name IN (" + p1Params + ")))";
        Assert.assertEquals(query, expected);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testEmptyFieldOnPrefix() throws Exception {
        FilterPredicate pred = new FilterPredicate(new Path.PathElement(Book.class, String.class, ""),
                Operator.PREFIX_CASE_INSENSITIVE, Arrays.asList("value"));
        FilterTranslator filterOp = new FilterTranslator();
        filterOp.apply(pred);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testEmptyFieldOnInfix() throws Exception {
        FilterPredicate pred = new FilterPredicate(new Path.PathElement(Book.class, String.class, ""),
                Operator.INFIX_CASE_INSENSITIVE, Arrays.asList("value"));
        FilterTranslator filterOp = new FilterTranslator();
        filterOp.apply(pred);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testEmptyFieldOnPostfix() throws Exception {
        FilterPredicate pred = new FilterPredicate(new Path.PathElement(Book.class, String.class, ""),
                Operator.POSTFIX_CASE_INSENSITIVE, Arrays.asList("value"));
        FilterTranslator filterOp = new FilterTranslator();
        filterOp.apply(pred);
    }

    @Test
    public void testCustomPredicate() throws Exception {

        FilterTranslator.JPQLPredicateGenerator generator = new FilterTranslator.JPQLPredicateGenerator() {
            @Override
            public String generate(String columnAlias, List<FilterPredicate.FilterParameter> parameters) {
                return "FOO";
            }
        };

        try {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, Author.class, "name", generator);

            FilterPredicate pred = new FilterPredicate(new Path.PathElement(Author.class, String.class, "name"),
                    Operator.INFIX_CASE_INSENSITIVE, Arrays.asList("value"));

            String actual = new FilterTranslator().apply(pred);
            Assert.assertEquals(actual, "FOO");
        } finally {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, Author.class, "name", null);
        }
    }

    @Test
    public void testCustomOperator() throws Exception {

        FilterTranslator.JPQLPredicateGenerator generator = new FilterTranslator.JPQLPredicateGenerator() {
            @Override
            public String generate(String columnAlias, List<FilterPredicate.FilterParameter> parameters) {
                return "FOO";
            }
        };

        FilterTranslator.JPQLPredicateGenerator old = FilterTranslator.lookupJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE);
        try {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, generator);

            FilterPredicate pred = new FilterPredicate(new Path.PathElement(Author.class, String.class, "name"),
                    Operator.INFIX_CASE_INSENSITIVE, Arrays.asList("value"));

            String actual = new FilterTranslator().apply(pred);
            Assert.assertEquals(actual, "FOO");
        } finally {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, old);
        }
    }
}
