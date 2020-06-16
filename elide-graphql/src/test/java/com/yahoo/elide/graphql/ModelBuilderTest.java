/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.EntityDictionary;

import example.Author;
import example.Book;
import example.Publisher;

import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelBuilderTest {
    private EntityDictionary dictionary;

    private static final String DATA = "data";
    private static final String FILTER = "filter";
    private static final String SORT = "sort";
    private static final String FIRST = "first";
    private static final String AFTER = "after";
    private static final String TYPE = "type";

    // Connection fields
    private static final String EDGES = "edges";
    private static final String NODE = "node";

    // Meta fields
    private static final String PAGE_INFO = "pageInfo";

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
    private static final String WEIGHT_LBS = "weightLbs";

    // TODO: We need more tests. I've updated the models to contain all of the situations below, but we should _esnure_
    // the generated result is exactly correct:
    //
    //   * Duplicate enums in same objects
    //   * Duplicate Set<Enum> across objects
    //   * Duplicate types across objects
    //   * Enum as map keys
    //   * Enum as map values
    //   * Duplicate maps of the same type
    //
    // This is all important for ensuring we don't duplicate typenames which is a requirement in the latest graphql-java

    public ModelBuilderTest() {
        dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
    }

    @Test
    public void testPageInfoObject() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, new NonEntityDictionary(), fetcher);

        GraphQLSchema schema = builder.build();

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(BOOK);
        assertNotNull(bookType.getFieldDefinition(PAGE_INFO));
    }

    @Test
    public void testRelationshipParameters() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, new NonEntityDictionary(), fetcher);

        GraphQLSchema schema = builder.build();
        GraphQLObjectType root = schema.getQueryType();
        assertNotNull(root);
        assertNotNull(root.getFieldDefinition(BOOK));

        /* The root 'book' should have all query parameters defined */
        GraphQLFieldDefinition bookField = root.getFieldDefinition(BOOK);
        assertNotNull(bookField.getArgument(DATA));
        assertNotNull(bookField.getArgument(FILTER));
        assertNotNull(bookField.getArgument(SORT));
        assertNotNull(bookField.getArgument(FIRST));
        assertNotNull(bookField.getArgument(AFTER));

        /* book.publisher is a 'to one' relationship so it should be missing all but the data parameter */
        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType("_node__" + BOOK);
        GraphQLFieldDefinition publisherField = bookType.getFieldDefinition("publisher");
        assertNotNull(publisherField.getArgument(DATA));
        assertNull(publisherField.getArgument(FILTER));
        assertNull(publisherField.getArgument(SORT));
        assertNull(publisherField.getArgument(FIRST));
        assertNull(publisherField.getArgument(AFTER));

        /* book.authors is a 'to many' relationship so it should have all query parameters defined */
        GraphQLFieldDefinition authorField = bookType.getFieldDefinition(AUTHORS);
        assertNotNull(authorField.getArgument(DATA));
        assertNotNull(authorField.getArgument(FILTER));
        assertNotNull(authorField.getArgument(SORT));
        assertNotNull(authorField.getArgument(FIRST));
        assertNotNull(authorField.getArgument(AFTER));
    }

    @Test
    public void testBuild() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ModelBuilder builder = new ModelBuilder(dictionary, new NonEntityDictionary(), fetcher);

        GraphQLSchema schema = builder.build();

        assertNotEquals(schema.getType(AUTHOR), null);
        assertNotEquals(schema.getType(BOOK), null);
        assertNotEquals(schema.getType(AUTHOR_INPUT), null);
        assertNotEquals(schema.getType(BOOK_INPUT), null);
        assertNotEquals(schema.getType("_root"), null);

        GraphQLObjectType bookType = getConnectedType((GraphQLObjectType) schema.getType(BOOK), null);
        GraphQLObjectType authorType = getConnectedType((GraphQLObjectType) schema.getType(AUTHOR), null);

        assertTrue(bookType.getFieldDefinition(TITLE).getType().equals(Scalars.GraphQLString));
        assertTrue(bookType.getFieldDefinition(GENRE).getType().equals(Scalars.GraphQLString));
        assertTrue(bookType.getFieldDefinition(LANGUAGE).getType().equals(Scalars.GraphQLString));
        assertTrue(bookType.getFieldDefinition(PUBLISH_DATE).getType().equals(Scalars.GraphQLLong));
        assertTrue(bookType.getFieldDefinition(WEIGHT_LBS).getType().equals(Scalars.GraphQLBigDecimal));

        GraphQLObjectType addressType = (GraphQLObjectType) authorType.getFieldDefinition("homeAddress").getType();
        assertTrue(addressType.getFieldDefinition("street1").getType().equals(Scalars.GraphQLString));
        assertTrue(addressType.getFieldDefinition("street2").getType().equals(Scalars.GraphQLString));


        GraphQLObjectType authorsType = (GraphQLObjectType) bookType.getFieldDefinition(AUTHORS).getType();
        GraphQLObjectType authorsNodeType = getConnectedType(authorsType, null);

        assertTrue(authorsNodeType.equals(authorType));

        assertTrue(authorType.getFieldDefinition(NAME).getType().equals(Scalars.GraphQLString));

        assertTrue(validateEnum(Author.AuthorType.class,
                (GraphQLEnumType) authorType.getFieldDefinition(TYPE).getType()));

        // Node type != connection type
        GraphQLObjectType booksNodeType = (GraphQLObjectType) authorType.getFieldDefinition(BOOKS).getType();
        assertFalse(booksNodeType.equals(bookType));

        GraphQLInputObjectType bookInputType = (GraphQLInputObjectType) schema.getType(BOOK_INPUT);
        GraphQLInputObjectType authorInputType = (GraphQLInputObjectType) schema.getType(AUTHOR_INPUT);

        assertTrue(bookInputType.getField(TITLE).getType().equals(Scalars.GraphQLString));
        assertTrue(bookInputType.getField(GENRE).getType().equals(Scalars.GraphQLString));
        assertTrue(bookInputType.getField(LANGUAGE).getType().equals(Scalars.GraphQLString));
        assertTrue(bookInputType.getField(PUBLISH_DATE).getType().equals(Scalars.GraphQLLong));

        GraphQLList authorsInputType = (GraphQLList) bookInputType.getField(AUTHORS).getType();
        assertTrue(authorsInputType.getWrappedType().equals(authorInputType));

        assertTrue(authorInputType.getField(NAME).getType().equals(Scalars.GraphQLString));

        GraphQLList booksInputType = (GraphQLList) authorInputType.getField(BOOKS).getType();
        assertTrue(booksInputType.getWrappedType().equals(bookInputType));
    }

    private GraphQLObjectType getConnectedType(GraphQLObjectType root, String connectionName) {
        GraphQLList edgesType = (GraphQLList) root.getFieldDefinition(EDGES).getType();
        GraphQLObjectType rootType =  (GraphQLObjectType)
                ((GraphQLObjectType) edgesType.getWrappedType()).getFieldDefinition(NODE).getType();
        if (connectionName == null) {
            return rootType;
        }
        return getConnectedType((GraphQLObjectType) rootType.getFieldDefinition(connectionName).getType(), null);
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
