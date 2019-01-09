/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.hibernate.hql.RelationshipImpl;
import com.yahoo.elide.core.hibernate.hql.SubCollectionFetchQueryBuilder;
import com.yahoo.elide.core.sort.Sorting;

import example.Author;
import example.Book;
import example.Chapter;
import example.Publisher;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubCollectionFetchQueryBuilderTest {
    private EntityDictionary dictionary;

    private static final String TITLE = "title";
    private static final String BOOKS = "books";
    private static final String NAME = "name";
    private static final String PUBLISHER = "publisher";
    private static final String PUB1 = "Pub1";

    @BeforeClass
    public void initialize() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Chapter.class);
    }

    @Test
    public void testSubCollectionFetch() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        RelationshipImpl relationship = new RelationshipImpl(
                Author.class,
                Book.class,
                BOOKS,
                author,
                Arrays.asList(book));

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        Assert.assertNull(query);
    }

    @Test
    public void testSubCollectionFetchWithSorting() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        RelationshipImpl relationship = new RelationshipImpl(
                Author.class,
                Book.class,
                BOOKS,
                author,
                Arrays.asList(book));

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleSorting(Optional.of(new Sorting(sorting)))
                .build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "WHERE example_Author__fetch=:example_Author__fetch order by example_Book.title asc";
        String actual = query.getQueryText();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSubCollectionFetchWithJoinFilter() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        RelationshipImpl relationship = new RelationshipImpl(
                Author.class,
                Book.class,
                BOOKS,
                author,
                Arrays.asList(book)
        );

        List<Path.PathElement>  publisherNamePath = Arrays.asList(
                new Path.PathElement(Book.class, Publisher.class, PUBLISHER),
                new Path.PathElement(Publisher.class, String.class, NAME)
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath),
                PUB1);

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleFilterExpression(Optional.of(publisherNamePredicate))
                .build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "LEFT JOIN example_Book.publisher example_Book_publisher  "
                + "WHERE example_Book_publisher.name IN (:books_publisher_name_XXX) AND example_Author__fetch=:example_Author__fetch ";
        String actual = query.getQueryText();
        actual = actual.replaceFirst(":publisher_name_\\w+_\\w+", ":books_publisher_name_XXX");

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSubCollectionFetchWithSortingAndFilters() {
        Author author = new Author();
        author.setId(1L);

        Book book = new Book();
        book.setId(2);

        RelationshipImpl relationship = new RelationshipImpl(
                Author.class,
                Book.class,
                BOOKS,
                author,
                Arrays.asList(book)
        );

        List<Path.PathElement>  publisherNamePath = Arrays.asList(
                new Path.PathElement(Book.class, Publisher.class, PUBLISHER),
                new Path.PathElement(Publisher.class, String.class, NAME)
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath),
                PUB1);

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship, dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleFilterExpression(Optional.of(publisherNamePredicate))
                .withPossibleSorting(Optional.of(new Sorting(sorting)))
                .build();

        String expected = "SELECT example_Book FROM example.Author example_Author__fetch "
                + "JOIN example_Author__fetch.books example_Book "
                + "LEFT JOIN example_Book.publisher example_Book_publisher  "
                + "WHERE example_Book_publisher.name IN (:publisher_name_XXX) AND example_Author__fetch=:example_Author__fetch  order by example_Book.title asc";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":publisher_name_\\w+", ":publisher_name_XXX");

        Assert.assertEquals(actual, expected);
    }
}
