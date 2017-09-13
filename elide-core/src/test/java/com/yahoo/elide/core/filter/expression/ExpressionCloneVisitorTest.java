/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import example.Author;
import example.Book;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests ExpressionCloneVisitor
 */
public class ExpressionCloneVisitorTest {

    private static final String SCIFI = "scifi";
    private static final String GENRE = "genre";
    private static final String NAME = "name";

    @Test
    public void testExpressionCopy() throws Exception {
        Path p1Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, Author.class, "authors"),
                new Path.PathElement(Author.class, String.class, NAME)
        ));
        FilterPredicate p1 = new FilterPredicate(p1Path, Operator.IN, Arrays.asList("foo", "bar"));

        Path p2Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, NAME)
        ));
        FilterPredicate p2 = new FilterPredicate(p2Path, Operator.IN, Arrays.asList("blah"));

        Path p3Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, GENRE)
        ));

        FilterPredicate p3 = new FilterPredicate(p3Path, Operator.IN, Arrays.asList(SCIFI));

        //P4 is a duplicate of P3
        Path p4Path = new Path(Arrays.asList(
                new Path.PathElement(Book.class, String.class, GENRE)
        ));

        FilterPredicate p4 = new FilterPredicate(p4Path, Operator.IN, Arrays.asList(SCIFI));

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        ExpressionCloneVisitor cloner = new ExpressionCloneVisitor();
        FilterExpression copy = not.accept(cloner);

        Assert.assertEquals(copy, not);
        Assert.assertTrue(copy != not);

        PredicateExtractionVisitor extractor = new PredicateExtractionVisitor(new ArrayList<>());
        List<FilterPredicate> predicates = (List) copy.accept(extractor);

        List<FilterPredicate> toCompare = Arrays.asList(p2, p3, p1, p4);
        for (int i = 0; i < predicates.size(); i++) {
            FilterPredicate predicateOriginal = toCompare.get(i);
            FilterPredicate predicateCopy = predicates.get(i);
            Assert.assertEquals(predicateCopy, predicateOriginal);
            Assert.assertTrue(predicateCopy != predicateOriginal);
        }
    }
}
