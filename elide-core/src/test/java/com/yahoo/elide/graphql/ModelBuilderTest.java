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
import graphql.schema.*;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelBuilderTest {
    EntityDictionary dictionary;

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

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType("book");
        Assert.assertNotNull(bookType.getFieldDefinition("__meta"));
        GraphQLObjectType metaObject = (GraphQLObjectType) bookType.getFieldDefinition("__meta").getType();
        Assert.assertNotNull(metaObject.getFieldDefinition("page"));
        GraphQLObjectType pageObject = (GraphQLObjectType) metaObject.getFieldDefinition("page").getType();
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
        Assert.assertNotNull(root.getFieldDefinition("book"));

        /* The root 'book' should have all query parameters defined */
        GraphQLFieldDefinition bookField = root.getFieldDefinition("book");
        Assert.assertNotNull(bookField.getArgument("id"));
        Assert.assertNotNull(bookField.getArgument("data"));
        Assert.assertNotNull(bookField.getArgument("filter"));
        Assert.assertNotNull(bookField.getArgument("sort"));
        Assert.assertNotNull(bookField.getArgument("first"));
        Assert.assertNotNull(bookField.getArgument("offset"));

        /* book.publisher is a 'to one' relationship so it should be missing all but the data parameter */
        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType("book");
        GraphQLFieldDefinition publisherField = bookType.getFieldDefinition("publisher");
        Assert.assertNull(publisherField.getArgument("id"));
        Assert.assertNotNull(publisherField.getArgument("data"));
        Assert.assertNull(publisherField.getArgument("filter"));
        Assert.assertNull(publisherField.getArgument("sort"));
        Assert.assertNull(publisherField.getArgument("first"));
        Assert.assertNull(publisherField.getArgument("offset"));

        /* book.authors is a 'to many' relationship so it should have all query parameters defined */
        GraphQLFieldDefinition authorField = bookType.getFieldDefinition("authors");
        Assert.assertNotNull(authorField.getArgument("id"));
        Assert.assertNotNull(authorField.getArgument("data"));
        Assert.assertNotNull(authorField.getArgument("filter"));
        Assert.assertNotNull(authorField.getArgument("sort"));
        Assert.assertNotNull(authorField.getArgument("first"));
        Assert.assertNotNull(authorField.getArgument("offset"));
    }

    @Test
    public void testBuild() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);

        GraphQLSchema schema = builder.build();

        Assert.assertNotEquals(schema.getType("author"), null);
        Assert.assertNotEquals(schema.getType("book"), null);
        Assert.assertNotEquals(schema.getType("authorInput"), null);
        Assert.assertNotEquals(schema.getType("bookInput"), null);
        Assert.assertNotEquals(schema.getType("root"), null);

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType("book");
        GraphQLObjectType authorType = (GraphQLObjectType) schema.getType("author");

        Assert.assertTrue(bookType.getFieldDefinition("id").getType().equals(Scalars.GraphQLID));
        Assert.assertTrue(bookType.getFieldDefinition("title").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookType.getFieldDefinition("genre").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookType.getFieldDefinition("language").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookType.getFieldDefinition("publishDate").getType().equals(Scalars.GraphQLLong));

        GraphQLObjectType addressType = (GraphQLObjectType) authorType.getFieldDefinition("homeAddress").getType();
        Assert.assertTrue(addressType.getFieldDefinition("street1").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(addressType.getFieldDefinition("street2").getType().equals(Scalars.GraphQLString));


        GraphQLList authorsType = (GraphQLList) bookType.getFieldDefinition("authors").getType();

        Assert.assertTrue(authorsType.getWrappedType().equals(authorType));

        Assert.assertTrue(authorType.getFieldDefinition("id").getType().equals(Scalars.GraphQLID));
        Assert.assertTrue(authorType.getFieldDefinition("name").getType().equals(Scalars.GraphQLString));

        Assert.assertTrue(validateEnum(Author.AuthorType.class,
                (GraphQLEnumType) authorType.getFieldDefinition("type").getType()));

        GraphQLList booksType = (GraphQLList) authorType.getFieldDefinition("books").getType();
        Assert.assertTrue(booksType.getWrappedType().equals(bookType));

        GraphQLInputObjectType bookInputType = (GraphQLInputObjectType) schema.getType("bookInput");
        GraphQLInputObjectType authorInputType = (GraphQLInputObjectType) schema.getType("authorInput");

        Assert.assertTrue(bookInputType.getField("id").getType().equals(Scalars.GraphQLID));
        Assert.assertTrue(bookInputType.getField("title").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookInputType.getField("genre").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookInputType.getField("language").getType().equals(Scalars.GraphQLString));
        Assert.assertTrue(bookInputType.getField("publishDate").getType().equals(Scalars.GraphQLLong));

        GraphQLList authorsInputType = (GraphQLList) bookInputType.getField("authors").getType();
        Assert.assertTrue(authorsInputType.getWrappedType().equals(authorInputType));

        Assert.assertTrue(authorInputType.getField("id").getType().equals(Scalars.GraphQLID));
        Assert.assertTrue(authorInputType.getField("name").getType().equals(Scalars.GraphQLString));

        GraphQLList booksInputType = (GraphQLList) authorInputType.getField("books").getType();
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
