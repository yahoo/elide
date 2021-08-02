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

    private static final String SUBSCRIPTION = "Subscription";
    private static final String TYPE_BOOK = "Book";
    private static final String TYPE_AUTHOR = "Author";
    private static final String FIELD_PUBLISHER = "publisher";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_GENRE = "genre";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_PUBLISH_DATE = "publishDate";
    private static final String FIELD_WEIGHT_LBS = "weightLbs";
    private static final String FIELD_AUTHORS = "authors";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";

    public SubscriptionModelBuilderTest() {
        dictionary = new EntityDictionary(Collections.emptyMap());

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Address.class);
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
        subscriptionType.getFieldDefinition("bookAdded").getType().equals(bookType);
        subscriptionType.getFieldDefinition("bookDeleted").getType().equals(bookType);
        subscriptionType.getFieldDefinition("bookUpdated").getType().equals(bookType);

        subscriptionType.getFieldDefinition("authorAdded").getType().equals(authorType);
        subscriptionType.getFieldDefinition("authorDeleted").getType().equals(authorType);
        subscriptionType.getFieldDefinition("authorUpdated").getType().equals(authorType);
    }

    @Test
    public void testModelTypes() {
        DataFetcher fetcher = mock(DataFetcher.class);
        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary,
                new NonEntityDictionary(), fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(TYPE_BOOK);
        GraphQLObjectType authorType = (GraphQLObjectType) schema.getType(TYPE_AUTHOR);

        GraphQLObjectType publisherType = (GraphQLObjectType) bookType.getFieldDefinition(FIELD_PUBLISHER).getType();

        assertNotNull(bookType);
        assertNotNull(authorType);
        assertNotNull(schema.getType(SUBSCRIPTION));

        //Test root type description fields.
        assertEquals("A GraphQL Book", bookType.getDescription());
        assertNull(authorType.getDescription());

        //Test non-root type description fields.
        assertEquals("A book publisher", publisherType.getDescription());

        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_TITLE).getType());
        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_GENRE).getType());
        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_LANGUAGE).getType());
        assertEquals(Scalars.GraphQLLong, bookType.getFieldDefinition(FIELD_PUBLISH_DATE).getType());
        assertEquals(Scalars.GraphQLBigDecimal, bookType.getFieldDefinition(FIELD_WEIGHT_LBS).getType());

        GraphQLObjectType addressType = (GraphQLObjectType) authorType.getFieldDefinition("homeAddress").getType();
        assertEquals(Scalars.GraphQLString, addressType.getFieldDefinition("street1").getType());
        assertEquals(Scalars.GraphQLString, addressType.getFieldDefinition("street2").getType());

        GraphQLObjectType authorsType = (GraphQLObjectType)
                ((GraphQLList) bookType.getFieldDefinition(FIELD_AUTHORS).getType()).getWrappedType();

        assertEquals(Scalars.GraphQLString, authorsType.getFieldDefinition(FIELD_NAME).getType());
        assertTrue(validateEnum(Author.AuthorType.class,
                (GraphQLEnumType) authorsType.getFieldDefinition(FIELD_TYPE).getType()));

    }
}