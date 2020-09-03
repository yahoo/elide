/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.dialect.CaseSensitivityStrategy;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.hibernate.hql.RootCollectionFetchQueryBuilder;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

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
import java.util.Optional;

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
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Chapter.class);
        dictionary.bindEntity(Editor.class);
        filterParser = new RSQLFilterDialect(dictionary, new CaseSensitivityStrategy.UseColumnCollation());
    }

    @Test
    public void testRootFetch() {
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Book.class, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book  LEFT JOIN FETCH example_Book.publisher example_Book_publisher  ";
        String actual = query.getQueryText();

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithSorting() {
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Book.class, dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleSorting(Optional.of(new Sorting(sorting)))
                .build();

        String expected = "SELECT example_Book FROM example.Book AS example_Book  "
                + "LEFT JOIN FETCH example_Book.publisher example_Book_publisher   order by example_Book.title asc";
        String actual = query.getQueryText();

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithJoinFilter() throws ParseException {

        final FilterExpression titlePredicate = filterParser.parseFilterExpression("books.chapters.title=in=('ABC','DEF')", Author.class, true);
        final FilterExpression publisherNamePredicate = filterParser.parseFilterExpression("books.publisher.name=in='Pub1'", Author.class, true);

        OrFilterExpression expression = new OrFilterExpression(titlePredicate, publisherNamePredicate);

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Author.class, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleFilterExpression(Optional.of(expression))
                .build();


        String expected =
                "SELECT example_Author FROM example.Author AS example_Author  "
                + "LEFT JOIN example_Author.books example_Author_books  "
                + "LEFT JOIN example_Author_books.chapters example_Book_chapters   "
                + "LEFT JOIN example_Author_books.publisher example_Book_publisher  "
                + "WHERE (example_Book_chapters.title IN (:books_chapters_title_XXX, :books_chapters_title_XXX) "
                + "OR example_Book_publisher.name IN (:books_publisher_name_XXX)) ";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_publisher_name_\\w\\w\\w\\w+", ":books_publisher_name_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testDistinctRootFetchWithToManyJoinFilterAndPagination() throws ParseException {
        final Pagination pagination = Pagination.fromOffsetAndLimit(10, 0, false);

        final FilterExpression titlePredicate = filterParser.parseFilterExpression("books.chapters.title=in=('ABC','DEF')", Author.class, true);
        final FilterExpression publisherNamePredicate = filterParser.parseFilterExpression("books.publisher.name=in='Pub1'", Author.class, true);

        OrFilterExpression expression = new OrFilterExpression(titlePredicate, publisherNamePredicate);

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Author.class, dictionary, new TestSessionWrapper());

        builder.withPossiblePagination(Optional.of(pagination));

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleFilterExpression(Optional.of(expression))
                .build();

        String expected =
                "SELECT DISTINCT example_Author FROM example.Author AS example_Author  "
                + "LEFT JOIN example_Author.books example_Author_books  "
                + "LEFT JOIN example_Author_books.chapters example_Book_chapters   "
                + "LEFT JOIN example_Author_books.publisher example_Book_publisher  "
                + "WHERE (example_Book_chapters.title IN (:books_chapters_title_XXX, :books_chapters_title_XXX) "
                + "OR example_Book_publisher.name IN (:books_publisher_name_XXX)) ";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_publisher_name_\\w\\w\\w\\w+", ":books_publisher_name_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testDistinctRootFetchWithToManyJoinFilterAndSortOverRelationshipAndPagination() throws ParseException {
        final FilterExpression titlePredicate = filterParser.parseFilterExpression("books.chapters.title=in=('ABC','DEF')", Author.class, true);
        final FilterExpression publisherNamePredicate = filterParser.parseFilterExpression("books.publisher.name=in='Pub1'", Author.class, true);
        final Pagination pagination = Pagination.fromOffsetAndLimit(10, 0, false);
        final Sorting sorting = Sorting.parseSortRule("-books.title");

        OrFilterExpression expression = new OrFilterExpression(titlePredicate, publisherNamePredicate);

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
            Author.class, dictionary, new TestSessionWrapper());
        builder.withPossibleFilterExpression(Optional.of(expression));
        builder.withPossiblePagination(Optional.of(pagination));
        builder.withPossibleSorting(Optional.of(sorting));

        assertThrows(InvalidValueException.class, builder::build);
    }

    @Test
    public void testRootFetchWithSortingAndFilters() {
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Book.class, dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);


        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleSorting(Optional.of(new Sorting(sorting)))
                .withPossibleFilterExpression(Optional.of(idPredicate))
                .build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book  LEFT JOIN FETCH example_Book.publisher example_Book_publisher"
                + "  WHERE example_Book.id IN (:id_XXX)  order by example_Book.title asc";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }


    @Test
    public void testSortingWithJoin() {
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Book.class, dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PUBLISHER + PERIOD + NAME, Sorting.SortOrder.asc);


        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleSorting(Optional.of(new Sorting(sorting)))
                .build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book "
                        + "LEFT JOIN FETCH example_Book.publisher example_Book_publisher "
                        + "order by example_Book_publisher.name asc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithRelationshipSortingAndFilters() {
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Book.class, dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PUBLISHER + PERIOD + EDITOR + PERIOD + FIRSTNAME, Sorting.SortOrder.desc);

        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);


        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleSorting(Optional.of(new Sorting(sorting)))
                .withPossibleFilterExpression(Optional.of(idPredicate))
                .build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book"
                        + " LEFT JOIN FETCH example_Book.publisher example_Book_publisher"
                        + " LEFT JOIN example_Book_publisher.editor example_Publisher_editor"
                        + " WHERE example_Book.id IN (:id_XXX)"
                        + " order by example_Publisher_editor.firstName desc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }
}
