/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import example.Author;
import example.Book;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
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

import static org.mockito.Mockito.mock;

public class ModelBuilderTest {
    EntityDictionary dictionary;

    @BeforeSuite
    public void init() {
        dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
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
