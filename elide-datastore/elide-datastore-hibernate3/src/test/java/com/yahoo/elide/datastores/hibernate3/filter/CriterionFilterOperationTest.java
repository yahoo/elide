/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.filter;

import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import example.AddressFragment;
import example.Author;
import example.Book;
import example.Person;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.mockito.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests CriteriaFilterOperation.
 */
public class CriterionFilterOperationTest {

    @Test
    public void testNestedCriterion() throws Exception {
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

        Criteria criteria = mock(Criteria.class);
        CriterionFilterOperation filterOp = new CriterionFilterOperation(criteria);

        ArgumentCaptor<Criterion> argument = ArgumentCaptor.forClass(Criterion.class);
        filterOp.apply(not);
        verify(criteria, times(1)).add(argument.capture());
        verify(criteria, times(1)).createAlias("authors", "book__authors");

        Assert.assertEquals(argument.getValue().toString(),
                "not name in (blah) or genre in (scifi) and book__authors.name in (foo, bar)");
    }

    @Test
    public void testMultipartAlias() throws Exception {
        List<Predicate.PathElement> p1Path = Arrays.asList(
            new Predicate.PathElement(Person.class, "person", AddressFragment.class, "address"),
            new Predicate.PathElement(AddressFragment.class, "addressFragment", AddressFragment.ZipCode.class, "zip"),
            new Predicate.PathElement(AddressFragment.ZipCode.class, "zipCode", String.class, "zip")
        );
        Predicate p1 = new Predicate(p1Path, Operator.IN, Arrays.asList("61820"));

        Criteria criteria = mock(Criteria.class);
        CriterionFilterOperation filterOp = new CriterionFilterOperation(criteria);

        ArgumentCaptor<Criterion> argument = ArgumentCaptor.forClass(Criterion.class);
        filterOp.apply((FilterExpression) p1);

        verify(criteria, times(1)).add(argument.capture());
        verify(criteria).createAlias("address", "person__address");
        verify(criteria).createAlias("address.zip", "person__address__zip");

        Assert.assertEquals(argument.getValue().toString(), "person__address__zip.zip in (61820)");
    }
}
