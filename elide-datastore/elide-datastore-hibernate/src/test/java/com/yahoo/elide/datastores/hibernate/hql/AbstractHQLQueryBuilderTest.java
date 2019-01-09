/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.InfixPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.hibernate.Query;
import com.yahoo.elide.core.hibernate.hql.AbstractHQLQueryBuilder;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import example.Author;
import example.Book;
import example.Chapter;
import example.Left;
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

    private static final String AUTHORS = "authors";
    private static final String BOOKS = "books";
    private static final String TITLE = "title";
    private static final String PUBLISHER = "publisher";
    private static final String GENRE = "genre";
    private static final String ABC = "ABC";
    private static final String DEF = "DEF";
    private static final String NAME = "name";

    public AbstractHQLQueryBuilderTest() {
        super(new EntityDictionary(new HashMap<>()), new TestSessionWrapper());
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Chapter.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Left.class);
    }


    @Override
    public Query build() {
        return null;
    }

    @Test
    public void testFilterJoinClause() {
        List<Path.PathElement> chapterTitlePath = Arrays.asList(
                new Path.PathElement(Author.class, Book.class, BOOKS),
                new Path.PathElement(Book.class, Chapter.class, "chapters"),
                new Path.PathElement(Chapter.class, String.class, TITLE)
        );

        FilterPredicate titlePredicate = new InPredicate(
                new Path(chapterTitlePath),
                ABC, DEF);

        FilterPredicate titlePredicateDuplicate = new InPredicate(
                new Path(chapterTitlePath),
                ABC, DEF);

        List<Path.PathElement>  publisherNamePath = Arrays.asList(
                new Path.PathElement(Author.class, Book.class, BOOKS),
                new Path.PathElement(Book.class, Publisher.class, PUBLISHER),
                new Path.PathElement(Publisher.class, String.class, NAME)
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath),
                "Pub1");

        OrFilterExpression orExpression = new OrFilterExpression(titlePredicate, publisherNamePredicate);
        AndFilterExpression andExpression = new AndFilterExpression(orExpression, titlePredicateDuplicate);

        String actual = getJoinClauseFromFilters(andExpression);
        String expected = " LEFT JOIN example_Author.books example_Author_books  "
                + "LEFT JOIN example_Author_books.chapters example_Book_chapters   "
                + "LEFT JOIN example_Author_books.publisher example_Book_publisher  ";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testFetchJoinClause() {
        String actual = extractToOneMergeJoins(Left.class, "right_alias");

        String expected = " LEFT JOIN FETCH right_alias.noUpdateOne2One  LEFT JOIN FETCH right_alias.one2one ";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSortClauseWithoutPrefix() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);
        sorting.put(GENRE, Sorting.SortOrder.desc);

        String actual = getSortClause(Optional.of(new Sorting(sorting)), Book.class, NO_ALIAS);

        String expected = " order by title asc,genre desc";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSortClauseWithPrefix() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);
        sorting.put(GENRE, Sorting.SortOrder.desc);

        String actual = getSortClause(Optional.of(new Sorting(sorting)), Book.class, USE_ALIAS);

        String expected = " order by example_Book.title asc,example_Book.genre desc";
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSortClauseWithJoin() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put(PUBLISHER + PERIOD + NAME, Sorting.SortOrder.asc);

        String actual = getSortClause(Optional.of(new Sorting(sorting)), Book.class, NO_ALIAS);

        String expected = " order by publisher.name asc";
        Assert.assertEquals(actual, expected);
    }

    @Test(expectedExceptions = InvalidValueException.class)
    public void testSortClauseWithInvalidJoin() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put(AUTHORS + PERIOD + NAME, Sorting.SortOrder.asc);

        getSortClause(Optional.of(new Sorting(sorting)), Book.class, NO_ALIAS);
    }

    @Test
    public void testSettingQueryParams() {
        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        Query query = mock(Query.class);
        FilterPredicate predicate = new InPredicate(idPath, ABC, DEF);
        supplyFilterQueryParameters(query, Arrays.asList(predicate));

        verify(query, times(2)).setParameter(anyString(), any());

        query = mock(Query.class);
        predicate = new InfixPredicate(idPath, ABC);
        supplyFilterQueryParameters(query, Arrays.asList(predicate));

        verify(query, times(1)).setParameter(anyString(), any());
    }

    @Test
    public void testSettingQueryPagination() {
        Query query = mock(Query.class);

        Optional<Pagination> previousPagination = pagination;

        Pagination paginationMock = mock(Pagination.class);
        when(paginationMock.getLimit()).thenReturn(10);
        when(paginationMock.getOffset()).thenReturn(50);

        pagination = Optional.of(paginationMock);

        try {
            addPaginationToQuery(query);
            verify(query, times(1)).setMaxResults(10);
            verify(query, times(1)).setFirstResult(50);
        } finally {
            pagination = previousPagination;
        }
    }
}
