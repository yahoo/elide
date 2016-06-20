/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.google.common.collect.Sets;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import example.Author;
import example.Book;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tests PredicateExtractionVisitor
 */
public class PredicateExtractionVisitorTest {

    @Test
    public void testPredicateExtraction() throws Exception {
        List<Predicate.PathElement> p1Path = Arrays.asList(
                new Predicate.PathElement(Book.class, "book", Author.class, "authors"),
                new Predicate.PathElement(Author.class, "author", String.class, "name")
        );
        Predicate p1 = new Predicate(p1Path, Operator.IN, Arrays.asList("foo", "bar"));

        List<Predicate.PathElement> p2Path = Arrays.asList(
                new Predicate.PathElement(Book.class, "book", String.class, "name")
        );
        Predicate p2 = new Predicate(p2Path, Operator.IN, Arrays.asList("blah"));

        List<Predicate.PathElement> p3Path = Arrays.asList(
                new Predicate.PathElement(Book.class, "book", String.class, "genre")
        );
        Predicate p3 = new Predicate(p3Path, Operator.IN, Arrays.asList("scifi"));

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and = new AndFilterExpression(or, p1);
        NotFilterExpression not = new NotFilterExpression(and);

        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();

        Set<Predicate> predicates = not.accept(visitor);

        Assert.assertEquals(predicates, Sets.newHashSet(p1, p2, p3));
    }
}
