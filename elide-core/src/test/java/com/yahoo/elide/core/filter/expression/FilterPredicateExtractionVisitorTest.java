/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.google.common.collect.Sets;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.books.Author;
import com.yahoo.books.Book;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tests PredicateExtractionVisitor
 */
public class FilterPredicateExtractionVisitorTest {

    @Test
    public void testPredicateExtraction() throws Exception {
        List<FilterPredicate.PathElement> p1Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, "book", Author.class, "authors"),
                new FilterPredicate.PathElement(Author.class, "author", String.class, "name")
        );
        FilterPredicate p1 = new FilterPredicate(p1Path, Operator.IN, Arrays.asList("foo", "bar"));

        List<FilterPredicate.PathElement> p2Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, "book", String.class, "name")
        );
        FilterPredicate p2 = new FilterPredicate(p2Path, Operator.IN, Arrays.asList("blah"));

        List<FilterPredicate.PathElement> p3Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, "book", String.class, "genre")
        );
        FilterPredicate p3 = new FilterPredicate(p3Path, Operator.IN, Arrays.asList("scifi"));

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and = new AndFilterExpression(or, p1);
        NotFilterExpression not = new NotFilterExpression(and);

        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();

        Set<FilterPredicate> filterPredicates = not.accept(visitor);

        Assert.assertEquals(filterPredicates, Sets.newHashSet(p1, p2, p3));
    }
}
