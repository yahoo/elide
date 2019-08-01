/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate.hql;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.hibernate.hql.AbstractHQLQueryBuilder;
import com.yahoo.elide.core.hibernate.hql.RelationshipImpl;
import com.yahoo.elide.core.hibernate.hql.SubCollectionPageTotalsQueryBuilder;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import com.yahoo.elide.models.example.Author;
import com.yahoo.elide.models.example.Book;
import com.yahoo.elide.models.example.Chapter;
import com.yahoo.elide.models.example.Publisher;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SubCollectionPageTotalsQueryBuilderTest {
    private EntityDictionary dictionary;

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

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder
                .build();

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        String expected =
                "SELECT COUNT(DISTINCT com_yahoo_elide_models_example_Author_books)  "
                + "FROM com.yahoo.elide.models.example.Author AS com_yahoo_elide_models_example_Author  "
                + "JOIN com_yahoo_elide_models_example_Author.books com_yahoo_elide_models_example_Author_books  "
                + "WHERE com_yahoo_elide_models_example_Author.id IN (:id_XXX)";

        Assert.assertEquals(actual, expected);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testSubCollectionPageTotalsWithSorting() {
        AbstractHQLQueryBuilder.Relationship relationship = mock(AbstractHQLQueryBuilder.Relationship.class);
        Sorting sorting = mock(Sorting.class);

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        builder.withPossibleSorting(Optional.of(sorting)).build();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testSubCollectionPageTotalsWithPagination() {
        AbstractHQLQueryBuilder.Relationship relationship = mock(AbstractHQLQueryBuilder.Relationship.class);
        Pagination pagination = mock(Pagination.class);

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(relationship,
                dictionary, new TestSessionWrapper());

        builder.withPossiblePagination(Optional.of(pagination));
    }

    @Test
    public void testSubCollectionPageTotalsWithJoinFilter() {
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
                new Path.PathElement(Publisher.class, String.class, "name")
        );

        FilterPredicate publisherNamePredicate = new InPredicate(
                new Path(publisherNamePath),
                "Pub1");

        SubCollectionPageTotalsQueryBuilder builder = new SubCollectionPageTotalsQueryBuilder(
                relationship, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleFilterExpression(Optional.of(publisherNamePredicate))
                .build();

        String expected =
                "SELECT COUNT(DISTINCT com_yahoo_elide_models_example_Author_books)  "
                + "FROM com.yahoo.elide.models.example.Author AS com_yahoo_elide_models_example_Author  "
                + "LEFT JOIN com_yahoo_elide_models_example_Author.books com_yahoo_elide_models_example_Author_books  "
                + "LEFT JOIN com_yahoo_elide_models_example_Author_books.publisher com_yahoo_elide_models_example_Book_publisher   "
                + "WHERE (com_yahoo_elide_models_example_Book_publisher.name IN (:books_publisher_name_XXX) "
                + "AND com_yahoo_elide_models_example_Author.id IN (:id_XXX))";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_publisher_name_\\w+", ":books_publisher_name_XXX");
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        Assert.assertEquals(actual, expected);
    }
}
