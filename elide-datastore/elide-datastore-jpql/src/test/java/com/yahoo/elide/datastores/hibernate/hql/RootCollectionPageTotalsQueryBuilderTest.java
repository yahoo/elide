/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.datastores.jpql.query.RootCollectionPageTotalsQueryBuilder;
import example.Author;
import example.Book;
import example.Chapter;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RootCollectionPageTotalsQueryBuilderTest {
    private EntityDictionary dictionary;

    private static final String TITLE = "title";
    private static final String BOOKS = "books";
    private static final String PUBLISHER = "publisher";

    @BeforeAll
    public void initialize() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Chapter.class);
    }

    @Test
    public void testRootFetch() {
        EntityProjection entityProjection = EntityProjection.builder().type(Book.class).build();
        RootCollectionPageTotalsQueryBuilder builder = new RootCollectionPageTotalsQueryBuilder(
                entityProjection, dictionary, new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
            "SELECT COUNT(DISTINCT example_Book) "
            + "FROM example.Book AS example_Book";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithSorting() {
        Sorting sorting = mock(Sorting.class);
        EntityProjection entityProjection = EntityProjection.builder()
                .type(Book.class)
                .sorting(sorting)
                .build();
        TestQueryWrapper query = (TestQueryWrapper) new RootCollectionPageTotalsQueryBuilder(
                entityProjection, dictionary, new TestSessionWrapper()
        )
                .build();
        String expected = "SELECT COUNT(DISTINCT example_Book) FROM example.Book AS example_Book";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithPagination() {
        PaginationImpl pagination = mock(PaginationImpl.class);
        EntityProjection entityProjection = EntityProjection.builder()
                .type(Book.class)
                .pagination(pagination)
                .build();
        TestQueryWrapper query = (TestQueryWrapper) new RootCollectionPageTotalsQueryBuilder(
                entityProjection, dictionary, new TestSessionWrapper()
        )
                .build();
        String expected = "SELECT COUNT(DISTINCT example_Book) FROM example.Book AS example_Book";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithJoinFilter() {
        List<Path.PathElement> chapterTitlePath = Arrays.asList(
                new Path.PathElement(Author.class, Book.class, BOOKS),
                new Path.PathElement(Book.class, Chapter.class, "chapters"),
                new Path.PathElement(Chapter.class, String.class, TITLE)
        );

        FilterPredicate titlePredicate = new InPredicate(
                new Path(chapterTitlePath),
                "ABC", "DEF");

        List<Path.PathElement>  publisherNamePath = Arrays.asList(
                new Path.PathElement(Author.class, Book.class, BOOKS),
                new Path.PathElement(Book.class, Publisher.class, PUBLISHER),
                new Path.PathElement(Publisher.class, String.class, "name")
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath), "Pub1");

        OrFilterExpression expression = new OrFilterExpression(titlePredicate, publisherNamePredicate);

        EntityProjection entityProjection = EntityProjection.builder().type(Author.class)
                .filterExpression(expression)
                .build();

        RootCollectionPageTotalsQueryBuilder builder = new RootCollectionPageTotalsQueryBuilder(
                entityProjection, dictionary, new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT COUNT(DISTINCT example_Author) FROM example.Author AS example_Author "
                + "LEFT JOIN example_Author.books example_Author_books "
                + "LEFT JOIN example_Author_books.chapters example_Author_books_chapters "
                + "LEFT JOIN example_Author_books.publisher example_Author_books_publisher "
                + "WHERE (example_Author_books_chapters.title IN "
                + "(:books_chapters_title_XXX, :books_chapters_title_XXX) "
                + "OR example_Author_books_publisher.name IN (:books_publisher_name_XXX))";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_publisher_name_\\w\\w\\w\\w+", ":books_publisher_name_XXX");

        assertEquals(expected, actual);
    }
}
