/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.graphql.ModelBuilderTest.validateEnum;
import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.graphql.NonEntityDictionary;
import example.Address;
import example.Author;
import example.Book;
import example.Preview;
import example.Publisher;
import org.junit.jupiter.api.Test;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.Collections;

public class SubscriptionModelBuilderTest {
    private EntityDictionary dictionary;

    private static final String FILTER = "filter";

    private static final String BOOK_ADDED = "bookAdded";
    private static final String BOOK_DELETED = "bookDeleted";
    private static final String BOOK_UPDATED = "bookUpdated";
    private static final String AUTHOR_ADDED = "authorAdded";
    private static final String AUTHOR_DELETED = "authorDeleted";
    private static final String AUTHOR_UPDATED = "authorUpdated";

    private static final String SUBSCRIPTION = "Subscription";
    private static final String TYPE_BOOK = "Book";
    private static final String TYPE_AUTHOR = "Author";
    private static final String TYPE_PREVIEW = "Preview";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_GENRE = "genre";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_PUBLISH_DATE = "publishDate";
    private static final String FIELD_AUTHORS = "authors";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";

    public SubscriptionModelBuilderTest() {
        dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Address.class);
        dictionary.bindEntity(Preview.class);
    }

    @Test
    public void testRootType() {
        DataFetcher fetcher = mock(DataFetcher.class);
        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary,
                new NonEntityDictionary(), fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();
        GraphQLObjectType subscriptionType = (GraphQLObjectType) schema.getType("Subscription");

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(TYPE_BOOK);
        GraphQLObjectType authorType = (GraphQLObjectType) schema.getType(TYPE_AUTHOR);

        assertEquals(bookType, subscriptionType.getFieldDefinition(BOOK_ADDED).getType());
        assertNotNull(subscriptionType.getFieldDefinition(BOOK_ADDED).getArgument(FILTER));

        assertNull(subscriptionType.getFieldDefinition(BOOK_DELETED));

        assertEquals(bookType, subscriptionType.getFieldDefinition(BOOK_UPDATED).getType());
        assertNotNull(subscriptionType.getFieldDefinition(BOOK_UPDATED).getArgument(FILTER));

        assertEquals(authorType, subscriptionType.getFieldDefinition(AUTHOR_ADDED).getType());
        assertNotNull(subscriptionType.getFieldDefinition(AUTHOR_ADDED).getArgument(FILTER));
        assertEquals(authorType, subscriptionType.getFieldDefinition(AUTHOR_DELETED).getType());
        assertNotNull(subscriptionType.getFieldDefinition(AUTHOR_DELETED).getArgument(FILTER));
        assertEquals(authorType, subscriptionType.getFieldDefinition(AUTHOR_UPDATED).getType());
        assertNotNull(subscriptionType.getFieldDefinition(AUTHOR_UPDATED).getArgument(FILTER));

        assertNull(subscriptionType.getFieldDefinition("publisherAdded"));
        assertNull(subscriptionType.getFieldDefinition("publisherDeleted"));
        assertNull(subscriptionType.getFieldDefinition("publisherUpdated"));
    }

    @Test
    public void testModelTypes() {
        DataFetcher fetcher = mock(DataFetcher.class);
        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary,
                new NonEntityDictionary(), fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(TYPE_BOOK);
        GraphQLObjectType authorType = (GraphQLObjectType) schema.getType(TYPE_AUTHOR);
        GraphQLObjectType previewType = (GraphQLObjectType) schema.getType(TYPE_PREVIEW);
        GraphQLObjectType publisherType = (GraphQLObjectType) schema.getType("publisher");

        assertNotNull(bookType);
        assertNotNull(authorType);
        assertNotNull(previewType);
        assertNull(publisherType);
        assertNotNull(schema.getType(SUBSCRIPTION));

        //Test root type description fields.
        assertEquals("A GraphQL Book", bookType.getDescription());
        assertNull(authorType.getDescription());

        //Verify Book Fields
        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_TITLE).getType());
        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_GENRE).getType());
        assertEquals(Scalars.GraphQLID, previewType.getFieldDefinition(FIELD_ID).getType());
        assertEquals(previewType,
                ((GraphQLList) bookType.getFieldDefinition("previews").getType()).getWrappedType());
        assertEquals(authorType,
                ((GraphQLList) bookType.getFieldDefinition(FIELD_AUTHORS).getType()).getWrappedType());
        assertEquals(5, bookType.getFieldDefinitions().size());

        //Verify fields without SubscriptionField are missing.
        assertNull(bookType.getFieldDefinition(FIELD_LANGUAGE));
        assertNull(bookType.getFieldDefinition(FIELD_PUBLISH_DATE));
        assertNull(bookType.getFieldDefinition("publisher"));

        //Verify Author Fields
        assertEquals(Scalars.GraphQLID, authorType.getFieldDefinition(FIELD_ID).getType());
        GraphQLObjectType addressType = (GraphQLObjectType) authorType.getFieldDefinition("homeAddress").getType();
        assertEquals(Scalars.GraphQLString, addressType.getFieldDefinition("street1").getType());
        assertEquals(Scalars.GraphQLString, addressType.getFieldDefinition("street2").getType());
        assertEquals(Scalars.GraphQLString, authorType.getFieldDefinition(FIELD_NAME).getType());
        assertTrue(validateEnum(Author.AuthorType.class,
                (GraphQLEnumType) authorType.getFieldDefinition(FIELD_TYPE).getType()));
        assertEquals(4, authorType.getFieldDefinitions().size());

        //Verify fields without SubscriptionField are missing.
        assertNull(bookType.getFieldDefinition("books"));

        //Verify Preview Fields
        assertEquals(Scalars.GraphQLID, previewType.getFieldDefinition(FIELD_ID).getType());
        assertEquals(1, previewType.getFieldDefinitions().size());

    }
}
