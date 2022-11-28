/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.google.common.collect.Sets;
import example.Author;
import example.Book;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Tests PredicateExtractionVisitor
 */
public class FilterPredicateExtractionVisitorTest {

    @Test
    public void testPredicateExtraction() throws Exception {
        Path p1Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, Author.class, "authors"),
                new Path.PathElement(Author.class, String.class, "name")
        ));
        FilterPredicate p1 = new InPredicate(p1Path, "foo", "bar");

        Path p2Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, "name")
        ));
        FilterPredicate p2 = new InPredicate(p2Path, "blah");

        Path p3Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, "genre")
        ));
        FilterPredicate p3 = new InPredicate(p3Path, "scifi");

        //P4 is a duplicate of P3
        Path p4Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, "genre")
        ));
        FilterPredicate p4 = new InPredicate(p4Path, "scifi");

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        //First test collecting the predicates in a Set
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();
        Collection<FilterPredicate> filterPredicates = not.accept(visitor);

        assertTrue(filterPredicates.containsAll(Sets.newHashSet(p1, p2, p3)));
        assertEquals(filterPredicates.size(), 3);

        //Second test collecting the predicates in a List
        visitor = new PredicateExtractionVisitor(new ArrayList<>());
        filterPredicates = not.accept(visitor);

        assertTrue(filterPredicates.containsAll(Arrays.asList(p1, p2, p3, p4)));
        assertEquals(filterPredicates.size(), 4);
    }
}
