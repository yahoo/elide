/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.hibernate.hql.RelationshipImpl;
import com.yahoo.elide.core.hibernate.hql.SubCollectionPageTotalsQueryBuilder;
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

public class SubCollectionPageTotalsQueryBuilderTest {
    private EntityDictionary dictionary;

    private static final String TITLE = "title";
    private static final String BOOKS = "books";
    private static final String PUBLISHER = "publisher";

    @BeforeClass
    public void initialize() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Chapter.class);
    }

    @Test
    public void testSubCollectionPageTotals() {
        RelationshipImpl relationship = new RelationshipImpl();
        relationship.setParentType(Author.class);
        relationship.setChildType(Book.class);
        relationship.setRelationshipName(BOOKS);

        Author author = new Author();
        author.setId(1);
        relationship.setParent(author);

        Book book = new Book();
        book.setId(2);
        relationship.setChildren(Arrays.asList(book));

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        String expected =
                "SELECT COUNT(DISTINCT example_Author_books)  "
                + "FROM example.Author AS example_Author  "
                + "JOIN example_Author.books example_Author_books  "
                + "WHERE example_Author.id IN (:id_XXX)";

        Assert.assertEquals(actual, expected);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testSubCollectionPageTotalsWithSorting() {
        RelationshipImpl relationship = new RelationshipImpl();
        relationship.setParentType(Author.class);
        relationship.setChildType(Book.class);
        relationship.setRelationshipName(BOOKS);

        Author author = new Author();
        author.setId(1);
        relationship.setParent(author);

        Book book = new Book();
        book.setId(2);
        relationship.setChildren(Arrays.asList(book));

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put(TITLE, Sorting.SortOrder.asc);

        builder.withSorting(new Sorting(sorting)).build();
    }

    @Test
    public void testSubCollectionPageTotalsWithJoinFilter() {
        RelationshipImpl relationship = new RelationshipImpl();
        relationship.setParentType(Author.class);
        relationship.setChildType(Book.class);
        relationship.setRelationshipName(BOOKS);

        Author author = new Author();
        author.setId(1);
        relationship.setParent(author);

        Book book = new Book();
        book.setId(2);
        relationship.setChildren(Arrays.asList(book));

        List<FilterPredicate.PathElement>  publisherNamePath = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, "book", Publisher.class, PUBLISHER),
                new FilterPredicate.PathElement(Publisher.class, PUBLISHER, String.class, "name")
        );

        FilterPredicate publisherNamePredicate = new FilterPredicate(
                publisherNamePath,
                Operator.IN, Arrays.asList("Pub1"));

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(
                relationship, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withFilterExpression(publisherNamePredicate)
                .build();

        String expected =
                "SELECT COUNT(DISTINCT example_Author_books)  "
                + "FROM example.Author AS example_Author  "
                + "JOIN example_Author.books example_Author_books  "
                + "JOIN example_Author_books.publisher example_Book_publisher   "
                + "WHERE (example_Book_publisher.name IN (:books_publisher_name_XXX) "
                + "AND example_Author.id IN (:id_XXX))";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_publisher_name_\\w+", ":books_publisher_name_XXX");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        Assert.assertEquals(actual, expected);
    }
}
