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

public class SubCollectionFetchQueryBuilderTest {
    private EntityDictionary dictionary;

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
        RelationshipImpl relationship = new RelationshipImpl();
        relationship.setParentType(Author.class);
        relationship.setChildType(Book.class);
        relationship.setRelationshipName("books");

        Author author = new Author();
        author.setId(1);
        relationship.setParent(author);

        Book book = new Book();
        book.setId(2);
        relationship.setChildren(Arrays.asList(book));

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        Assert.assertNull(query);
    }

    @Test
    public void testSubCollectionFetchWithSorting() {
        RelationshipImpl relationship = new RelationshipImpl();
        relationship.setParentType(Author.class);
        relationship.setChildType(Book.class);
        relationship.setRelationshipName("books");

        Author author = new Author();
        author.setId(1);
        relationship.setParent(author);

        Book book = new Book();
        book.setId(2);
        relationship.setChildren(Arrays.asList(book));

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put("title", Sorting.SortOrder.asc);

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withSorting(new Sorting(sorting))
                .build();

        String expected = " order by title asc";
        String actual = query.getQueryText();

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSubCollectionFetchWithJoinFilter() {
        RelationshipImpl relationship = new RelationshipImpl();
        relationship.setParentType(Author.class);
        relationship.setChildType(Book.class);
        relationship.setRelationshipName("books");

        Author author = new Author();
        author.setId(1);
        relationship.setParent(author);

        Book book = new Book();
        book.setId(2);
        relationship.setChildren(Arrays.asList(book));

        List<FilterPredicate.PathElement>  publisherNamePath = Arrays.asList(
                new FilterPredicate.PathElement(Author.class, "author", Book.class, "books"),
                new FilterPredicate.PathElement(Book.class, "book", Publisher.class, "publisher"),
                new FilterPredicate.PathElement(Publisher.class, "publisher", String.class, "name")
        );

        FilterPredicate publisherNamePredicate = new FilterPredicate(
                publisherNamePath,
                Operator.IN, Arrays.asList("Pub1"));

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withFilterExpression(publisherNamePredicate)
                .build();

        String expected = "WHERE books.publisher.name IN (:books_publisher_name_XXX) ";
        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_publisher_name_\\w+", ":books_publisher_name_XXX");

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testSubCollectionFetchWithSortingAndFilters() {
                RelationshipImpl relationship = new RelationshipImpl();
        relationship.setParentType(Author.class);
        relationship.setChildType(Book.class);
        relationship.setRelationshipName("books");

        Author author = new Author();
        author.setId(1);
        relationship.setParent(author);

        Book book = new Book();
        book.setId(2);
        relationship.setChildren(Arrays.asList(book));

        List<FilterPredicate.PathElement>  publisherNamePath = Arrays.asList(
                new FilterPredicate.PathElement(Book.class, "book", Publisher.class, "publisher"),
                new FilterPredicate.PathElement(Publisher.class, "publisher", String.class, "name")
        );

        FilterPredicate publisherNamePredicate = new FilterPredicate(
                publisherNamePath,
                Operator.IN, Arrays.asList("Pub1"));

        SubCollectionFetchQueryBuilder builder = new SubCollectionFetchQueryBuilder(
                relationship, dictionary, new TestSessionWrapper());

        Map<String, Sorting.SortOrder> sorting = new HashMap<>();
        sorting.put("title", Sorting.SortOrder.asc);

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withFilterExpression(publisherNamePredicate)
                .withSorting(new Sorting(sorting))
                .build();

        String expected = "WHERE publisher.name IN (:publisher_name_XXX)  order by title asc";
        String actual = query.getQueryText();
        actual = actual.replaceFirst(":publisher_name_\\w+", ":publisher_name_XXX");

        Assert.assertEquals(actual, expected);
    }
}
