/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

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

    @Test
    public void testExpressionCopy() throws Exception {
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

        //P4 is a duplicate of P3
        List<FilterPredicate.PathElement> p4Path = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, "book", String.class, "genre")
        );
        FilterPredicate p4 = new FilterPredicate(p3Path, Operator.IN, Arrays.asList("scifi"));

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
