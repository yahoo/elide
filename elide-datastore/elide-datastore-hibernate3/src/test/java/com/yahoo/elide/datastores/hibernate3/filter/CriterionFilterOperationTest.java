/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
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
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests CriteriaFilterOperation.
 */
public class CriterionFilterOperationTest {

    @Test
    public void testNestedCriterion() throws Exception {
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
        List<FilterPredicate.PathElement> p1Path = Arrays.asList(
            new FilterPredicate.PathElement(Person.class, "person", AddressFragment.class, "address"),
            new FilterPredicate.PathElement(AddressFragment.class, "addressFragment", AddressFragment.ZipCode.class, "zip"),
            new FilterPredicate.PathElement(AddressFragment.ZipCode.class, "zipCode", String.class, "zip")
        );
        FilterPredicate p1 = new FilterPredicate(p1Path, Operator.IN, Arrays.asList("61820"));

        Criteria criteria = mock(Criteria.class);
        CriterionFilterOperation filterOp = new CriterionFilterOperation(criteria);

        ArgumentCaptor<Criterion> argument = ArgumentCaptor.forClass(Criterion.class);
        filterOp.apply((FilterExpression) p1);

        verify(criteria, times(1)).add(argument.capture());
        verify(criteria).createAlias("address", "person__address");
        verify(criteria).createAlias("address.zip", "person__address__zip");

        Assert.assertEquals(argument.getValue().toString(), "person__address__zip.zip in (61820)");
    }

    @DataProvider(name = "like_queries")
    Object [][] likeQueryPredicateDataProvider() {
        List<FilterPredicate.PathElement> p1Path = Arrays.asList(
                new FilterPredicate.PathElement(Person.class, "person", String.class, "name")
        );
        return new Object[][]{
                {new FilterPredicate(p1Path, Operator.POSTFIX, Arrays.asList("%bi%ll%")), "name like %\\%bi\\%ll\\%"},
                {new FilterPredicate(p1Path, Operator.PREFIX, Arrays.asList("%bi%ll%")), "name like \\%bi\\%ll\\%%"},
                {new FilterPredicate(p1Path, Operator.INFIX, Arrays.asList("%bi%ll%")), "name like %\\%bi\\%ll\\%%"}
        };
    }

    @Test(dataProvider = "like_queries")
    public void testEscapingPercentageSign(FilterPredicate predicate, String outputCriteria) throws Exception {
        Criteria criteria = mock(Criteria.class);
        CriterionFilterOperation filterOp = new CriterionFilterOperation(criteria);

        ArgumentCaptor<Criterion> argument = ArgumentCaptor.forClass(Criterion.class);
        filterOp.apply((FilterExpression) predicate);

        verify(criteria, times(1)).add(argument.capture());
        Assert.assertEquals(argument.getValue().toString(), outputCriteria);
    }
}
