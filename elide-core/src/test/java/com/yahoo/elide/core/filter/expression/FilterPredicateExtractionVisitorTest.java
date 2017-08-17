/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.google.common.collect.Sets;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.FilterPredicate;
import example.Author;
import example.Book;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests PredicateExtractionVisitor
 */
public class FilterPredicateExtractionVisitorTest {

    @Test
    public void testPredicateExtraction() throws Exception {
        List<FilterPredicate.PathElement> p1Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, Author.class, "authors"),
                new FilterPredicate.PathElement(Author.class, String.class, "name")
        );
        FilterPredicate p1 = new FilterPredicate(p1Path, Operator.IN, Arrays.asList("foo", "bar"));

        List<FilterPredicate.PathElement> p2Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, String.class, "name")
        );
        FilterPredicate p2 = new FilterPredicate(p2Path, Operator.IN, Arrays.asList("blah"));

        List<FilterPredicate.PathElement> p3Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, String.class, "genre")
        );
        FilterPredicate p3 = new FilterPredicate(p3Path, Operator.IN, Arrays.asList("scifi"));

        //P4 is a duplicate of P3
        List<FilterPredicate.PathElement> p4Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, String.class, "genre")
        );
        FilterPredicate p4 = new FilterPredicate(p4Path, Operator.IN, Arrays.asList("scifi"));

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        //First test collecting the predicates in a Set
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();
        Collection<FilterPredicate> filterPredicates = not.accept(visitor);

        Assert.assertTrue(filterPredicates.containsAll(Sets.newHashSet(p1, p2, p3)));
        Assert.assertEquals(3, filterPredicates.size());

        //Second test collecting the predicates in a List
        visitor = new PredicateExtractionVisitor(new ArrayList<>());
        filterPredicates = not.accept(visitor);

        Assert.assertTrue(filterPredicates.containsAll(Arrays.asList(p1, p2, p3, p4)));
        Assert.assertEquals(4, filterPredicates.size());
    }
}
