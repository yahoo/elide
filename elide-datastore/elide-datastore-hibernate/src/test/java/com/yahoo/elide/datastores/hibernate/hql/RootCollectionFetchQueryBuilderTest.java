/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.CaseSensitivityStrategy;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.hibernate.hql.RootCollectionFetchQueryBuilder;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import example.Author;
import example.Book;
import example.Chapter;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RootCollectionFetchQueryBuilderTest {
    private EntityDictionary dictionary;

    private static final String TITLE = "title";
    private static final String PUBLISHER = "publisher";
    private static final String EDITOR = "editor";
    private static final String PERIOD = ".";
    private static final String NAME = "name";
    private static final String FIRSTNAME = "firstName";
    private RSQLFilterDialect filterParser;

    @BeforeAll
    public void initialize() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Chapter.class);
        dictionary.bindEntity(Editor.class);
        filterParser = new RSQLFilterDialect(dictionary, new CaseSensitivityStrategy.UseColumnCollation());
    }

    @Test
    public void testRootFetch() {
        EntityProjection entityProjection = EntityProjection.builder().type(Book.class).build();
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithSorting() {
        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        EntityProjection entityProjection = EntityProjection
                .builder()
                .type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Book AS example_Book "
                + "order by example_Book.title asc";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithJoinFilter() throws ParseException {

        final FilterExpression titlePredicate = filterParser.parseFilterExpression("books.chapters.title=in=('ABC','DEF')", ClassType.of(Author.class), true);
        final FilterExpression publisherNamePredicate = filterParser.parseFilterExpression("books.publisher.name=in='Pub1'", ClassType.of(Author.class), true);

        OrFilterExpression expression = new OrFilterExpression(titlePredicate, publisherNamePredicate);

        EntityProjection entityProjection = EntityProjection
                .builder()
                .type(Author.class)
                .filterExpression(expression)
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();


        String expected =
                "SELECT example_Author FROM example.Author AS example_Author  "
                + "LEFT JOIN example_Author.books example_Author_books  "
                + "LEFT JOIN example_Author_books.chapters example_Author_books_chapters   "
                + "LEFT JOIN example_Author_books.publisher example_Author_books_publisher   "
                + "WHERE (example_Author_books_chapters.title IN (:books_chapters_title_XXX, :books_chapters_title_XXX) "
                + "OR example_Author_books_publisher.name IN (:books_publisher_name_XXX)) ";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_publisher_name_\\w\\w\\w\\w+", ":books_publisher_name_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testDistinctRootFetchWithToManyJoinFilterAndPagination() throws ParseException {
        final Pagination pagination = new PaginationImpl(Book.class, 0, 10, 10, 10, false, false);

        final FilterExpression titlePredicate = filterParser.parseFilterExpression("books.chapters.title=in=('ABC','DEF')", ClassType.of(Author.class), true);
        final FilterExpression publisherNamePredicate = filterParser.parseFilterExpression("books.publisher.name=in='Pub1'", ClassType.of(Author.class), true);

        OrFilterExpression expression = new OrFilterExpression(titlePredicate, publisherNamePredicate);

        EntityProjection entityProjection = EntityProjection
                .builder()
                .type(Author.class)
                .pagination(pagination)
                .filterExpression(expression)
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT DISTINCT example_Author FROM example.Author AS example_Author "
                + "LEFT JOIN example_Author.books example_Author_books "
                + "LEFT JOIN example_Author_books.chapters example_Author_books_chapters "
                + "LEFT JOIN example_Author_books.publisher example_Author_books_publisher "
                + "WHERE (example_Author_books_chapters.title IN (:books_chapters_title_XXX, :books_chapters_title_XXX) "
                + "OR example_Author_books_publisher.name IN (:books_publisher_name_XXX))";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_publisher_name_\\w\\w\\w\\w+", ":books_publisher_name_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithSortingAndFilters() {
        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .filterExpression(idPredicate)
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );


        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book"
                + " WHERE example_Book.id IN (:id_XXX) order by example_Book.title asc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testSortingWithJoin() {
        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PUBLISHER + PERIOD + NAME, Sorting.SortOrder.asc);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book "
                        + "LEFT JOIN example_Book.publisher example_Book_publisher "
                        + "order by example_Book_publisher.name asc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithRelationshipSortingAndFilters() {
        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PUBLISHER + PERIOD + EDITOR + PERIOD + FIRSTNAME, Sorting.SortOrder.desc);

        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .filterExpression(idPredicate)
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book"
                        + " LEFT JOIN example_Book.publisher example_Book_publisher"
                        + " LEFT JOIN example_Book_publisher.editor example_Book_publisher_editor"
                        + " WHERE example_Book.id IN (:id_XXX)"
                        + " order by example_Book_publisher_editor.firstName desc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithToOneRelationIncluded() {
        EntityProjection entityProjection = EntityProjection.builder().type(Book.class)
                .relationship(
                        Relationship.builder().name(PUBLISHER).projection(
                                EntityProjection.builder().type(Publisher.class).build()
                        ).build()
                )
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }
}
