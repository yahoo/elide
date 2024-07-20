/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.dialect.CaseSensitivityStrategy;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Pagination.Direction;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.jpql.porting.Query;
import com.yahoo.elide.datastores.jpql.porting.SingleResultQuery;
import com.yahoo.elide.datastores.jpql.query.CursorEncoder;
import com.yahoo.elide.datastores.jpql.query.JacksonCursorEncoder;
import com.yahoo.elide.datastores.jpql.query.RootCollectionFetchQueryBuilder;
import example.Author;
import example.Book;
import example.Chapter;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RootCollectionFetchQueryBuilderTest {
    private EntityDictionary dictionary;

    private static final String TITLE = "title";
    private static final String GENRE = "genre";
    private static final String PUBLISHER = "publisher";
    private static final String EDITOR = "editor";
    private static final String PERIOD = ".";
    private static final String NAME = "name";
    private static final String FIRSTNAME = "firstName";
    private static final String PRICE = "price";
    private static final String TOTAL = "total";
    private RSQLFilterDialect filterParser;
    private CursorEncoder cursorEncoder = new JacksonCursorEncoder();

    @BeforeAll
    public void initialize() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Chapter.class);
        dictionary.bindEntity(Editor.class);
        filterParser = RSQLFilterDialect.builder()
                .dictionary(dictionary)
                .caseSensitivityStrategy(new CaseSensitivityStrategy.UseColumnCollation())
                .build();
    }

    @Test
    public void testRootFetch() {
        EntityProjection entityProjection = EntityProjection.builder().type(Book.class).build();
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithId() {
        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);

        EntityProjection entityProjection = EntityProjection
                .builder()
                .type(Book.class)
                .filterExpression(idPredicate)
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        Query query = builder.build();
        assertTrue(query instanceof SingleResultQuery);
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
                new TestSessionWrapper(),
                cursorEncoder
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
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();


        String expected =
                "SELECT DISTINCT example_Author FROM example.Author AS example_Author  "
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
                new TestSessionWrapper(),
                cursorEncoder
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
                new TestSessionWrapper(),
                cursorEncoder
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
                new TestSessionWrapper(),
                cursorEncoder
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
                new TestSessionWrapper(),
                cursorEncoder
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
    public void testRootFetchWithRelationshipSortingAndFiltersAndKeysetPagination() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put(PUBLISHER + PERIOD + EDITOR + PERIOD + FIRSTNAME, Sorting.SortOrder.desc);
        sorting.put(TITLE, Sorting.SortOrder.asc);
        sorting.put(GENRE, Sorting.SortOrder.asc);

        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .filterExpression(idPredicate)
                .pagination(new PaginationImpl(Book.class, null, 2, 10, 10, false, false, null,
                        cursorEncoder.encode(Map.of("id", "2")), Direction.FORWARD))
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Book AS example_Book"
                + " LEFT JOIN example_Book.publisher example_Book_publisher"
                + " LEFT JOIN example_Book_publisher.editor example_Book_publisher_editor"
                + " WHERE example_Book.id IN (:id_XXX)"
                + " AND "
                + "(example_Book.title > :_keysetParameter_0 OR "
                + "(example_Book.title = :_keysetParameter_0 AND example_Book.genre > :_keysetParameter_1) OR "
                + "(example_Book.title = :_keysetParameter_0 AND example_Book.genre = :_keysetParameter_1 AND example_Book.id > :_keysetParameter_2))"
                + " order by example_Book_publisher_editor.firstName desc,example_Book.title asc,example_Book.genre asc,example_Book.id asc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithRelationshipSortingAndFiltersAndKeysetPaginationBackward() {
        Map<String, Sorting.SortOrder> sorting = new LinkedHashMap<>();
        sorting.put(PUBLISHER + PERIOD + EDITOR + PERIOD + FIRSTNAME, Sorting.SortOrder.desc);
        sorting.put(TITLE, Sorting.SortOrder.asc);
        sorting.put(GENRE, Sorting.SortOrder.asc);

        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .filterExpression(idPredicate)
                .pagination(new PaginationImpl(Book.class, null, 2, 10, 10, false, false, null,
                        cursorEncoder.encode(Map.of("id", "2")), Direction.BACKWARD))
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Book AS example_Book"
                + " LEFT JOIN example_Book.publisher example_Book_publisher"
                + " LEFT JOIN example_Book_publisher.editor example_Book_publisher_editor"
                + " WHERE example_Book.id IN (:id_XXX)"
                + " AND "
                + "(example_Book.title < :_keysetParameter_0 OR "
                + "(example_Book.title = :_keysetParameter_0 AND example_Book.genre < :_keysetParameter_1) OR "
                + "(example_Book.title = :_keysetParameter_0 AND example_Book.genre = :_keysetParameter_1 AND example_Book.id < :_keysetParameter_2))"
                + " order by example_Book_publisher_editor.firstName desc,example_Book.title desc,example_Book.genre desc,example_Book.id desc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithRelationshipAndFiltersAndKeysetPagination() {
        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .sorting(new SortingImpl(Collections.emptyMap(), Book.class, dictionary))
                .filterExpression(idPredicate)
                .pagination(new PaginationImpl(Book.class, null, 2, 10, 10, false, false, null,
                        cursorEncoder.encode(Map.of("id", "2")), Direction.FORWARD))
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Book AS example_Book"
                + " WHERE example_Book.id IN (:id_XXX)"
                + " AND "
                + "(example_Book.id > :_keysetParameter_0)"
                + " order by example_Book.id asc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithRelationshipAndFiltersAndKeysetPaginationBackward() {
        Path.PathElement idPath = new Path.PathElement(Book.class, Chapter.class, "id");

        FilterPredicate idPredicate = new InPredicate(idPath, 1);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .sorting(new SortingImpl(Collections.emptyMap(), Book.class, dictionary))
                .filterExpression(idPredicate)
                .pagination(new PaginationImpl(Book.class, null, 2, 10, 10, false, false, null,
                        cursorEncoder.encode(Map.of("id", "2")), Direction.BACKWARD))
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Book AS example_Book"
                + " WHERE example_Book.id IN (:id_XXX)"
                + " AND "
                + "(example_Book.id < :_keysetParameter_0)"
                + " order by example_Book.id desc";

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
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT example_Book FROM example.Book AS example_Book LEFT JOIN FETCH example_Book.publisher";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testRootFetchWithRelationshipSortingFiltersAndPagination() {
        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PUBLISHER + PERIOD + EDITOR + PERIOD + FIRSTNAME, Sorting.SortOrder.desc);

        Path.PathElement idBook = new Path.PathElement(Book.class, Chapter.class, "chapters");
        Path.PathElement idChapter = new Path.PathElement(Chapter.class, long.class, "id");
        Path idPath = new Path(List.of(new Path.PathElement[]{idBook, idChapter}));

        FilterPredicate chapterIdPredicate = new InPredicate(idPath, 1);

        PaginationImpl pagination = new PaginationImpl(ClassType.of(Book.class), 0, 3, 10, 10, true, false);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .pagination(pagination)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .filterExpression(chapterIdPredicate)
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        assertThrows(InvalidValueException.class, () -> {
            TestQueryWrapper build = (TestQueryWrapper) builder.build();
        });
    }

    @Test
    public void testRootFetchWithRelationshipSortingFiltersAndPaginationOnEmbedded() {
        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PRICE + PERIOD + TOTAL, Sorting.SortOrder.desc);

        Path.PathElement idBook = new Path.PathElement(Book.class, Chapter.class, "chapters");
        Path.PathElement idChapter = new Path.PathElement(Chapter.class, long.class, "id");
        Path idPath = new Path(List.of(new Path.PathElement[]{idBook, idChapter}));

        FilterPredicate chapterIdPredicate = new InPredicate(idPath, 1);

        PaginationImpl pagination = new PaginationImpl(ClassType.of(Book.class), 0, 3, 10, 10, true, false);

        EntityProjection entityProjection = EntityProjection
                .builder().type(Book.class)
                .pagination(pagination)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .filterExpression(chapterIdPredicate)
                .build();

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                entityProjection,
                dictionary,
                new TestSessionWrapper(),
                cursorEncoder
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT DISTINCT example_Book FROM example.Book AS example_Book"
                        + " LEFT JOIN example_Book.chapters example_Book_chapters"
                        + " WHERE example_Book_chapters.id IN (:chapters_id_XXX)"
                        + " order by example_Book.price.total desc";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":chapters_id_\\w+", ":chapters_id_XXX");

        assertEquals(expected, actual);
    }
}
