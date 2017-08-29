/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.EntityDictionary;

import example.Author;
import example.Book;
import example.Publisher;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelBuilderTest {
    private EntityDictionary dictionary;

    private static final String DATA = "data";
    private static final String FILTER = "filter";
    private static final String SORT = "sort";
    private static final String FIRST = "first";
    private static final String OFFSET = "offset";
    private static final String META = "__meta";
    private static final String PAGE = "page";
    private static final String TYPE = "type";

    private static final String BOOK = "book";
    private static final String BOOKS = "books";
    private static final String BOOK_INPUT = "bookInput";
    private static final String NAME = "name";
    private static final String AUTHOR = "author";
    private static final String AUTHOR_INPUT = "authorInput";
    private static final String AUTHORS = "authors";
    private static final String TITLE = "title";
    private static final String PUBLISH_DATE = "publishDate";
    private static final String GENRE = "genre";
    private static final String LANGUAGE = "language";

    @BeforeSuite
    public void init() {
        dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
    }

    @Test
    public void testMetaObject() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);

        GraphQLSchema schema = builder.build();

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(BOOK);
        Assert.assertNotNull(bookType.getFieldDefinition(META));
        GraphQLObjectType metaObject = (GraphQLObjectType) bookType.getFieldDefinition(META).getType();
        Assert.assertNotNull(metaObject.getFieldDefinition(PAGE));
        GraphQLObjectType pageObject = (GraphQLObjectType) metaObject.getFieldDefinition(PAGE).getType();
        Assert.assertNotNull(pageObject.getFieldDefinition("totalPages"));
        Assert.assertNotNull(pageObject.getFieldDefinition("totalRecords"));
    }

    @Test
    public void testRelationshipParameters() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);

        GraphQLSchema schema = builder.build();
        GraphQLObjectType root = schema.getQueryType();
        Assert.assertNotNull(root);
        Assert.assertNotNull(root.getFieldDefinition(BOOK));

        /* The root 'book' should have all query parameters defined */
        GraphQLFieldDefinition bookField = root.getFieldDefinition(BOOK);
        Assert.assertNotNull(bookField.getArgument(DATA));
        Assert.assertNotNull(bookField.getArgument(FILTER));
        Assert.assertNotNull(bookField.getArgument(SORT));
        Assert.assertNotNull(bookField.getArgument(FIRST));
        Assert.assertNotNull(bookField.getArgument(OFFSET));

        /* book.publisher is a 'to one' relationship so it should be missing all but the data parameter */
        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(BOOK);
        GraphQLFieldDefinition publisherField = bookType.getFieldDefinition("publisher");
        Assert.assertNotNull(publisherField.getArgument(DATA));
        Assert.assertNull(publisherField.getArgument(FILTER));
        Assert.assertNull(publisherField.getArgument(SORT));
        Assert.assertNull(publisherField.getArgument(FIRST));
        Assert.assertNull(publisherField.getArgument(OFFSET));

        /* book.authors is a 'to many' relationship so it should have all query parameters defined */
        GraphQLFieldDefinition authorField = bookType.getFieldDefinition(AUTHORS);
        Assert.assertNotNull(authorField.getArgument(DATA));
        Assert.assertNotNull(authorField.getArgument(FILTER));
        Assert.assertNotNull(authorField.getArgument(SORT));
        Assert.assertNotNull(authorField.getArgument(FIRST));
        Assert.assertNotNull(authorField.getArgument(OFFSET));
    }

    @Test
    public void testBuild() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);

        GraphQLSchema schema = builder.build();

        Assert.assertNotEquals(schema.getType(AUTHOR), null);
        Assert.assertNotEquals(schema.getType(BOOK), null);
        Assert.assertNotEquals(schema.getType(AUTHOR_INPUT), null);
        Assert.assertNotEquals(schema.getType(BOOK_INPUT), null);
        Assert.assertNotEquals(schema.getType("root"), null);

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(BOOK);
        GraphQLObjectType authorType = (GraphQLObjectType) schema.getType(AUTHOR);

        Assert.assertTrue(bookType.getFieldDefinition(TITLE).getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookType.getFieldDefinition(GENRE).getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookType.getFieldDefinition(LANGUAGE).getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookType.getFieldDefinition(PUBLISH_DATE).getType().equals(Scalars.GraphQLLong));

        GraphQLObjectType addressType = (GraphQLObjectType) authorType.getFieldDefinition("homeAddress").getType();
        Assert.assertTrue(addressType.getFieldDefinition("street1").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(addressType.getFieldDefinition("street2").getType().equals(Scalars.GraphQLString));


        GraphQLList authorsType = (GraphQLList) bookType.getFieldDefinition(AUTHORS).getType();

        Assert.assertTrue(authorsType.getWrappedType().equals(authorType));

        Assert.assertTrue(authorType.getFieldDefinition(NAME).getType().equals(Scalars.GraphQLString));

        Assert.assertTrue(validateEnum(Author.AuthorType.class,
                (GraphQLEnumType) authorType.getFieldDefinition(TYPE).getType()));

        GraphQLList booksType = (GraphQLList) authorType.getFieldDefinition(BOOKS).getType();
        Assert.assertTrue(booksType.getWrappedType().equals(bookType));

        GraphQLInputObjectType bookInputType = (GraphQLInputObjectType) schema.getType(BOOK_INPUT);
        GraphQLInputObjectType authorInputType = (GraphQLInputObjectType) schema.getType(AUTHOR_INPUT);

        Assert.assertTrue(bookInputType.getField(TITLE).getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookInputType.getField(GENRE).getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookInputType.getField(LANGUAGE).getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookInputType.getField(PUBLISH_DATE).getType().equals(Scalars.GraphQLLong));

        GraphQLList authorsInputType = (GraphQLList) bookInputType.getField(AUTHORS).getType();
        Assert.assertTrue(authorsInputType.getWrappedType().equals(authorInputType));

        Assert.assertTrue(authorInputType.getField(NAME).getType().equals(Scalars.GraphQLString));

        GraphQLList booksInputType = (GraphQLList) authorInputType.getField(BOOKS).getType();
        Assert.assertTrue(booksInputType.getWrappedType().equals(bookInputType));
    }

    private boolean validateEnum(Class<?> expected, GraphQLEnumType actual) {
        Enum [] values = (Enum []) expected.getEnumConstants();
        Set<String> enumNames = actual.getValues().stream()
                .map((value) -> value.getName())
                .collect(Collectors.toSet());

        for (Enum value : values) {
            if (! enumNames.contains(value.name())) {
                return false;
            }
        }

        return true;
    }
}
