/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.hibernate.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.filter.predicates.InPredicate;
import com.paiondata.elide.core.pagination.PaginationImpl;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.request.Relationship;
import com.paiondata.elide.core.request.Sorting;
import com.paiondata.elide.core.sort.SortingImpl;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.datastores.jpql.query.RelationshipImpl;
import com.paiondata.elide.datastores.jpql.query.SubCollectionFetchQueryBuilder;
import example.Author;
import example.Book;
import example.Chapter;
import example.Editor;
import example.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubCollectionFetchQueryBuilderTest {

    private EntityDictionary dictionary;

    private static final String AUTHORS = "authors";
    private static final String TITLE = "title";
    private static final String BOOKS = "books";
    private static final String NAME = "name";
    private static final String PUBLISHER = "publisher";
    private static final String PERIOD = ".";
    private static final String PUB1 = "Pub1";
    private static final String EDITOR = "editor";
    private static final String FIRSTNAME = "firstName";

    @BeforeAll
    public void initialize() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Chapter.class);
        dictionary.bindEntity(Editor.class);
    }

    @Test
    public void testSubCollectionFetch() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class).build();
        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();


        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        assertNull(query);
    }

    @Test
    public void testSubCollectionFetchWithIncludedRelation() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class)
                .relationship(
                        Relationship.builder().name(PUBLISHER).projection(
                                EntityProjection.builder().type(Publisher.class).build()
                        ).build()
                )
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();

        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book LEFT JOIN FETCH example_Book.publisher "
                + "WHERE example_Author__fetch=:example_Author__fetch order by example_Book.title asc";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testSubCollectionFetchWithIncludedToManyRelation() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class)
                .relationship(
                        Relationship.builder().name(AUTHORS).projection(
                                EntityProjection.builder().type(Author.class).build()
                        ).build()
                )
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();

        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "WHERE example_Author__fetch=:example_Author__fetch order by example_Book.title asc";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testSubCollectionFetchWithSorting() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();
        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "WHERE example_Author__fetch=:example_Author__fetch order by example_Book.title asc";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testSubCollectionFetchWithJoinFilter() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        List<Path.PathElement>  publisherNamePath = Arrays.asList(
                new Path.PathElement(Book.class, Publisher.class, PUBLISHER),
                new Path.PathElement(Publisher.class, String.class, NAME)
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath),
                PUB1);


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

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "LEFT JOIN example_Book.publisher example_Book_publisher  "
                + "WHERE example_Book_publisher.name IN (:books_publisher_name_XXX) AND example_Author__fetch=:example_Author__fetch ";
        String actual = query.getQueryText();
        actual = actual.replaceFirst(":publisher_name_\\w+_\\w+", ":books_publisher_name_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testDistinctSubCollectionFetchWithToManyJoinFilter() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        List<Path.PathElement>  authorNamePath = Arrays.asList(
                new Path.PathElement(Book.class, Author.class, AUTHORS),
                new Path.PathElement(Author.class, String.class, NAME)
        );

        FilterPredicate authorNamePredicate = new InPredicate(
                new Path(authorNamePath),
                PUB1);


        EntityProjection entityProjection = EntityProjection.builder()
                .type(Book.class)
                .filterExpression(authorNamePredicate)
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();

        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT DISTINCT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "LEFT JOIN example_Book.authors example_Book_authors  "
                + "WHERE example_Book_authors.name "
                + "IN (:books_authors_name_XXX) AND example_Author__fetch=:example_Author__fetch ";
        String actual = query.getQueryText();
        actual = actual.replaceFirst(":authors_name_\\w+_\\w+", ":books_authors_name_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testDistinctSubCollectionFetchWithToManyJoinFilterAndSortOverRelationship() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        List<Path.PathElement>  authorNamePath = Arrays.asList(
                new Path.PathElement(Book.class, Author.class, AUTHORS),
                new Path.PathElement(Author.class, String.class, NAME)
        );

        FilterPredicate authorNamePredicate = new InPredicate(
                new Path(authorNamePath),
                PUB1);

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PUBLISHER + PERIOD + EDITOR + PERIOD + FIRSTNAME, Sorting.SortOrder.desc);

        EntityProjection entityProjection = EntityProjection.builder()
                .type(Book.class)
                .filterExpression(authorNamePredicate)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();

        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        assertThrows(InvalidValueException.class, () -> {
            TestQueryWrapper build = (TestQueryWrapper) builder.build();
        });
    }

    @Test
    public void testSubCollectionFetchWithSortingAndFilters() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        List<Path.PathElement>  publisherNamePath = Arrays.asList(
                new Path.PathElement(Book.class, Publisher.class, PUBLISHER),
                new Path.PathElement(Publisher.class, String.class, NAME)
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath),
                PUB1);

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class)
                .filterExpression(publisherNamePredicate)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();


        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "LEFT JOIN example_Book.publisher example_Book_publisher "
                + "WHERE example_Book_publisher.name IN (:publisher_name_XXX) AND example_Author__fetch=:example_Author__fetch order by example_Book.title asc";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":publisher_name_\\w+", ":publisher_name_XXX");
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }

    @Test
    public void testFetchJoinExcludesParent() {
        Publisher publisher = new Publisher();
        publisher.setId(1);

        Book book = new Book();
        book.setId(2);

        Pagination pagination = new PaginationImpl(Book.class, 0, 10, 10, 10, false, false);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class).pagination(pagination).build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();

        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Publisher.class),
                publisher,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Publisher example_Publisher__fetch "
                + "JOIN example_Publisher__fetch.books example_Book "
                + "WHERE example_Publisher__fetch=:example_Publisher__fetch";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":publisher_name_\\w+", ":publisher_name_XXX");

        assertEquals(expected, actual);
    }

    @Test
    public void testSubCollectionFetchWithRelationshipSorting() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(PUBLISHER + PERIOD + NAME, Sorting.SortOrder.asc);

        EntityProjection entityProjection = EntityProjection.builder().type(Book.class)
                .sorting(new SortingImpl(sorting, Book.class, dictionary))
                .build();

        Relationship relationshipProjection = Relationship.builder().name(BOOKS).projection(entityProjection).build();

        RelationshipImpl relationship = new RelationshipImpl(
                ClassType.of(Author.class),
                author,
                relationshipProjection
        );

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship,
                dictionary,
                new TestSessionWrapper()
        );

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "LEFT JOIN example_Book.publisher example_Book_publisher "
                + "WHERE example_Author__fetch=:example_Author__fetch order by example_Book_publisher.name asc";
        String actual = query.getQueryText();
        actual = actual.trim().replaceAll(" +", " ");

        assertEquals(expected, actual);
    }
}
