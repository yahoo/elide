/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.filter.predicates.NotEmptyPredicate;
import com.yahoo.elide.core.type.ClassType;

import example.Author;
import example.Book;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests Building a HQL Filter.
 */
public class FilterTranslatorTest {

    private EntityDictionary dictionary;

    public FilterTranslatorTest() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
    }

    @Test
    public void testHQLQueryVisitor() throws Exception {
        List<Path.PathElement> p0Path = Arrays.asList(
                new Path.PathElement(Book.class, Author.class, "authors")
        );
        FilterPredicate p0 = new NotEmptyPredicate(new Path(p0Path));

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
        AndFilterExpression and1 = new AndFilterExpression(p0, p1);
        AndFilterExpression and2 = new AndFilterExpression(or, and1);
        NotFilterExpression not = new NotFilterExpression(and2);

        FilterTranslator filterOp = new FilterTranslator(dictionary);
        String query = filterOp.apply(not, false);
        query = query.trim().replaceAll(" +", " ");

        String p1Params = p1.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String p2Params = p2.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String p3Params = p3.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String expected = "NOT (((name IN (" + p2Params + ") OR genre IN (" + p3Params + ")) "
                + "AND (authors IS NOT EMPTY AND authors.name IN (" + p1Params + "))))";
        assertEquals(expected, query);
    }

    @Test
    public void testBetweenOperator() throws Exception {
        List<Path.PathElement> authorId = Arrays.asList(
                new Path.PathElement(Book.class, Author.class, "authors"),
                new Path.PathElement(Author.class, Long.class, "id")
        );
        List<Path.PathElement> publishDate = Arrays.asList(
                new Path.PathElement(Book.class, Long.class, "publishDate")
        );
        FilterPredicate authorPred = new FilterPredicate(new Path(authorId), Operator.BETWEEN, Arrays.asList(1, 15));
        FilterPredicate publishPred = new FilterPredicate(new Path(publishDate), Operator.NOTBETWEEN, Arrays.asList(1, 15));


        AndFilterExpression andFilter = new AndFilterExpression(authorPred, publishPred);


        FilterTranslator filterOp = new FilterTranslator(dictionary);
        String query = filterOp.apply(andFilter, false);


        String authorP1 = authorPred.getParameters().get(0).getPlaceholder();
        String authorP2 = authorPred.getParameters().get(1).getPlaceholder();


        String publishP1 = publishPred.getParameters().get(0).getPlaceholder();
        String publishP2 = publishPred.getParameters().get(1).getPlaceholder();

        String expected = "(authors.id BETWEEN " + authorP1 + " AND " + authorP2 + " AND "
                + "publishDate NOT BETWEEN " + publishP1 + " AND " + publishP2 + ")";
        assertEquals(expected, query);


        // Assert excepetion if parameter length is not 2
        assertThrows(IllegalArgumentException.class,
                () -> filterOp.apply(
                        new FilterPredicate(new Path(authorId), Operator.BETWEEN, Arrays.asList(3))
                ));
    }

    @Test
    public void testMemberOfOperator() throws Exception {
        List<Path.PathElement> path = Arrays.asList(
                new Path.PathElement(Book.class, String.class, "awards")
        );
        FilterPredicate p1 = new FilterPredicate(new Path(path), Operator.HASMEMBER, Arrays.asList("awards1"));
        FilterPredicate p2 = new FilterPredicate(new Path(path), Operator.HASNOMEMBER, Arrays.asList("awards2"));

        AndFilterExpression and = new AndFilterExpression(p1, p2);

        FilterTranslator filterOp = new FilterTranslator(dictionary);
        String query = filterOp.apply(and, false);

        String p1Params = p1.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String p2Params = p2.getParameters().stream()
                .map(FilterPredicate.FilterParameter::getPlaceholder).collect(Collectors.joining(", "));
        String expected = "(" + p1Params + " MEMBER OF awards "
                + "AND " + p2Params + " NOT MEMBER OF awards)";
        assertEquals(expected, query);
    }

    @Test
    public void testEmptyFieldOnPrefix() throws Exception {
        FilterPredicate pred = new FilterPredicate(new Path.PathElement(Book.class, String.class, ""),
                Operator.PREFIX_CASE_INSENSITIVE, Arrays.asList("value"));
        FilterTranslator filterOp = new FilterTranslator(dictionary);
        assertThrows(InvalidValueException.class, () -> filterOp.apply(pred));
    }

    @Test
    public void testEmptyFieldOnInfix() throws Exception {
        FilterPredicate pred = new FilterPredicate(new Path.PathElement(Book.class, String.class, ""),
                Operator.INFIX_CASE_INSENSITIVE, Arrays.asList("value"));
        FilterTranslator filterOp = new FilterTranslator(dictionary);
        assertThrows(InvalidValueException.class, () -> filterOp.apply(pred));
    }

    @Test
    public void testEmptyFieldOnPostfix() throws Exception {
        FilterPredicate pred = new FilterPredicate(new Path.PathElement(Book.class, String.class, ""),
                Operator.POSTFIX_CASE_INSENSITIVE, Arrays.asList("value"));
        FilterTranslator filterOp = new FilterTranslator(dictionary);
        assertThrows(InvalidValueException.class, () -> filterOp.apply(pred));
    }

    @Test
    public void testCustomPredicate() throws Exception {

        JPQLPredicateGenerator generator = (predicate, aliasGenerator) -> "FOO";

        try {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, ClassType.of(Author.class), "name", generator);

            FilterPredicate pred = new FilterPredicate(new Path.PathElement(Author.class, String.class, "name"),
                    Operator.INFIX_CASE_INSENSITIVE, Arrays.asList("value"));

            String actual = new FilterTranslator(dictionary).apply(pred);
            assertEquals("FOO", actual);
        } finally {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, ClassType.of(Author.class), "name", null);
        }
    }

    @Test
    public void testCustomOperator() throws Exception {

        JPQLPredicateGenerator generator = (predicate, aliasGenerator) -> "FOO";

        JPQLPredicateGenerator old = FilterTranslator.lookupJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE);
        try {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, generator);

            FilterPredicate pred = new FilterPredicate(new Path.PathElement(Author.class, String.class, "name"),
                    Operator.INFIX_CASE_INSENSITIVE, Arrays.asList("value"));

            String actual = new FilterTranslator(dictionary).apply(pred);
            assertEquals("FOO", actual);
        } finally {
            FilterTranslator.registerJPQLGenerator(Operator.INFIX_CASE_INSENSITIVE, old);
        }
    }
}
