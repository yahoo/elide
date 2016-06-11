/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.filter.expression.AndExpression;
import com.yahoo.elide.core.filter.expression.NotExpression;
import com.yahoo.elide.core.filter.expression.OrExpression;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests Building a HQL Filter.
 */
public class HQLFilterOperationTest {

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

        OrExpression or = new OrExpression(p2, p3);
        AndExpression and = new AndExpression(or, p1);
        NotExpression not = new NotExpression(and);

        HQLFilterOperation filterOp = new HQLFilterOperation();
        String query = filterOp.apply(not);

        Assert.assertEquals(query, "WHERE NOT (((name IN (:name) OR genre IN (:genre)) AND name IN (:name)))");
    }
}
