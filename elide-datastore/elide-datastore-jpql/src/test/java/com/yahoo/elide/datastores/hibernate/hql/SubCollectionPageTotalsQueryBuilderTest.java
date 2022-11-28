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
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.jpql.query.RelationshipImpl;
import com.yahoo.elide.datastores.jpql.query.SubCollectionPageTotalsQueryBuilder;
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
public class SubCollectionPageTotalsQueryBuilderTest {

    private EntityDictionary dictionary;

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
    public void testSubCollectionPageTotals() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class).build();
        Relationship relationshipProjection = Relationship.builder().projection(entityProjection).name(BOOKS).build();
        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(
                relationship, dictionary, new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder
                .build();

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        String expected =
                "SELECT COUNT(DISTINCT example_Author_books) "
                + "FROM example.Author AS example_Author "
                + "JOIN example_Author.books example_Author_books "
                + "WHERE example_Author.id IN (:id_XXX)";

        assertEquals(expected, actual);
    }

    @Test
    public void testSubCollectionPageTotalsWithSorting() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        Sorting sorting = mock(Sorting.class);

        EntityProjection entityProjection = EntityProjection.builder()
                .type(Book.class)
                .sorting(sorting)
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();
        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        TestQueryWrapper query = (TestQueryWrapper) new SubCollectionPageTotalsQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        ).build();

        String expected =
                "SELECT COUNT(DISTINCT example_Author_books) "
                        + "FROM example.Author AS example_Author "
                        + "JOIN example_Author.books example_Author_books "
                        + "WHERE example_Author.id IN (:id_XXX)";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");
        assertEquals(expected, actual);
    }

    @Test
    public void testSubCollectionPageTotalsWithPagination() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        PaginationImpl pagination = mock(PaginationImpl.class);
        EntityProjection entityProjection = EntityProjection.builder()
                .type(Book.class)
                .pagination(pagination)
                .build();
        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();
        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        TestQueryWrapper query = (TestQueryWrapper) new SubCollectionPageTotalsQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        ).build();

        String expected =
                "SELECT COUNT(DISTINCT example_Author_books) "
                        + "FROM example.Author AS example_Author "
                        + "JOIN example_Author.books example_Author_books "
                        + "WHERE example_Author.id IN (:id_XXX)";

        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");
        assertEquals(expected, actual);
    }

    @Test
    public void testSubCollectionPageTotalsWithJoinFilter() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        List<Path.PathElement>  publisherNamePath = Arrays.asList(
                new Path.PathElement(Book.class, Publisher.class, PUBLISHER),
                new Path.PathElement(Publisher.class, String.class, "name")
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath),
                "Pub1");

        EntityProjection entityProjection = EntityProjection.builder()
                .type(Book.class)
                .filterExpression(publisherNamePredicate)
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();

        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected =
                "SELECT COUNT(DISTINCT example_Author_books) "
                + "FROM example.Author AS example_Author "
                + "LEFT JOIN example_Author.books example_Author_books "
                + "LEFT JOIN example_Author_books.publisher example_Author_books_publisher "
                + "WHERE (example_Author_books_publisher.name IN (:books_publisher_name_XXX) "
                + "AND example_Author.id IN (:id_XXX))";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_publisher_name_\\w+", ":books_publisher_name_XXX");
        actual = actual.trim().replaceAll(" +", " ");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        assertEquals(expected, actual);
    }
}
