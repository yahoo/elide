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
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.hibernate.hql.RootCollectionFetchQueryBuilder;
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
import java.util.Map;
import java.util.Optional;

public class RootCollectionFetchQueryBuilderTest {
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
    public void testRootFetch() {
        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Book.class, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder.build();

        String expected = "SELECT com_yahoo_elide_models_example_Book FROM com.yahoo.elide.models.example.Book AS com_yahoo_elide_models_example_Book  ";
        String actual = query.getQueryText();

        Assert.assertEquals(actual, expected);
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

        String expected = "SELECT com_yahoo_elide_models_example_Book FROM com.yahoo.elide.models.example.Book AS com_yahoo_elide_models_example_Book   order by com_yahoo_elide_models_example_Book.title asc";
        String actual = query.getQueryText();

        Assert.assertEquals(actual, expected);
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

        RootCollectionFetchQueryBuilder builder = new RootCollectionFetchQueryBuilder(
                Author.class, dictionary, new TestSessionWrapper());

        TestQueryWrapper query = (TestQueryWrapper) builder
                .withPossibleFilterExpression(Optional.of(expression))
                .build();


        String expected =
                "SELECT com_yahoo_elide_models_example_Author FROM com.yahoo.elide.models.example.Author AS com_yahoo_elide_models_example_Author  "
                + "LEFT JOIN com_yahoo_elide_models_example_Author.books com_yahoo_elide_models_example_Author_books  "
                + "LEFT JOIN com_yahoo_elide_models_example_Author_books.chapters com_yahoo_elide_models_example_Book_chapters   "
                + "LEFT JOIN com_yahoo_elide_models_example_Author_books.publisher com_yahoo_elide_models_example_Book_publisher  "
                + "WHERE (com_yahoo_elide_models_example_Book_chapters.title IN (:books_chapters_title_XXX, :books_chapters_title_XXX) "
                + "OR com_yahoo_elide_models_example_Book_publisher.name IN (:books_publisher_name_XXX)) ";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_chapters_title_\\w\\w\\w\\w+", ":books_chapters_title_XXX");
        actual = actual.replaceFirst(":books_publisher_name_\\w\\w\\w\\w+", ":books_publisher_name_XXX");

        Assert.assertEquals(actual, expected);
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
                "SELECT com_yahoo_elide_models_example_Book FROM com.yahoo.elide.models.example.Book AS com_yahoo_elide_models_example_Book  "
                + "WHERE com_yahoo_elide_models_example_Book.id IN (:id_XXX)  order by com_yahoo_elide_models_example_Book.title asc";

        String actual = query.getQueryText();
        actual = actual.replaceFirst(":id_\\w+", ":id_XXX");

        Assert.assertEquals(actual, expected);
    }
}
