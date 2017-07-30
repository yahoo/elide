/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.hql.AbstractHQLQueryBuilder;
import com.yahoo.elide.core.sort.Sorting;
import example.Author;
import example.Book;
import example.Chapter;
import example.Publisher;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AbstractHQLQueryBuilderTest extends AbstractHQLQueryBuilder {

    public AbstractHQLQueryBuilderTest() {
        super(new EntityDictionary(new HashMap<>()), new TestSessionWrapper());
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Chapter.class);
        dictionary.bindEntity(Publisher.class);
    }


    @Override
    public Query build() {
        return null;
    }

    @Test
    public void testFilterJoinClause() {
        List<FilterPredicate.PathElement> chapterTitlePath = Arrays.asList(
                new FilterPredicate.PathElement(Author.class, "author", Book.class, "books"),
                new FilterPredicate.PathElement(Book.class, "book", Chapter.class, "chapters"),
                new FilterPredicate.PathElement(Chapter.class, "chapter", String.class, "title")
        );

        FilterPredicate titlePredicate = new FilterPredicate(
                chapterTitlePath,
                Operator.IN, Arrays.asList("ABC", "DEF"));

        FilterPredicate titlePredicateDuplicate = new FilterPredicate(
                chapterTitlePath,
                Operator.IN, Arrays.asList("ABC", "DEF"));

        List<FilterPredicate.PathElement>  publisherNamePath = Arrays.asList(
                new FilterPredicate.PathElement(Author.class, "author", Book.class, "books"),
                new FilterPredicate.PathElement(Book.class, "book", Publisher.class, "publisher"),
                new FilterPredicate.PathElement(Publisher.class, "publisher", String.class, "name")
        );

        FilterPredicate publisherNamePredicate = new FilterPredicate(
                publisherNamePath,
                Operator.IN, Arrays.asList("Pub1"));

        OrFilterExpression orExpression = new OrFilterExpression(titlePredicate, publisherNamePredicate);
        AndFilterExpression andExpression = new AndFilterExpression(orExpression, titlePredicateDuplicate);

        String actual = getJoinClauseFromFilters(andExpression);
        String expected = " JOIN example_Author.books example_Author_books  "
                + "JOIN example_Author_books.chapters example_Book_chapters   "
                + "JOIN example_Author_books.publisher example_Book_publisher  ";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSortClauseWithoutPrefix() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put("title", Sorting.SortOrder.asc);
        sorting.put("genre", Sorting.SortOrder.desc);

        String actual = getSortClause(Optional.of(new Sorting(sorting)), Book.class, false);

        String expected = " order by title asc,genre desc";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSortClauseWithPrefix() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put("title", Sorting.SortOrder.asc);
        sorting.put("genre", Sorting.SortOrder.desc);

        String actual = getSortClause(Optional.of(new Sorting(sorting)), Book.class, true);

        String expected = " order by example_Book.title asc,example_Book.genre desc";
        Assert.assertEquals(actual, expected);
    }
}
