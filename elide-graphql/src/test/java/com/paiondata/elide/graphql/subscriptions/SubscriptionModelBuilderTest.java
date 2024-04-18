/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.paiondata.elide.graphql.ModelBuilderTest.validateEnum;
import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.graphql.GraphQLFieldDefinitionCustomizer;
import com.paiondata.elide.graphql.GraphQLScalars;
import com.paiondata.elide.graphql.GraphQLSettings;
import com.paiondata.elide.graphql.NonEntityDictionary;
import com.paiondata.elide.graphql.annotation.GraphQLDescription;

import example.Address;
import example.Author;
import example.Book;
import example.Preview;
import example.Publisher;
import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldDefinition.Builder;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

public class SubscriptionModelBuilderTest {
    private EntityDictionary dictionary;

    private static final String FILTER = "filter";

    private static final String BOOK = "book";
    private static final String AUTHOR = "author";
    private static final String PREVIEW = "preview";
    private static final String TOPIC = "topic";

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
        DataFetcher<?> fetcher = mock(DataFetcher.class);
        GraphQLFieldDefinitionCustomizer graphqlFieldDefinitionCustomizer = (Builder fieldDefinition,
                Type<?> parentClass, Type<?> attributeClass, String attribute, DataFetcher<?> dataFetcher,
                EntityDictionary entityDictionary) -> {
            GraphQLDescription description = entityDictionary.getAttributeOrRelationAnnotation(parentClass,
                    GraphQLDescription.class, attribute);
            if (description != null) {
                fieldDefinition.description(description.value());
            }
        };
        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary,
                new NonEntityDictionary(new DefaultClassScanner(), CoerceUtil::lookup),
                ElideSettings.builder().settings(GraphQLSettings.builder().graphqlFieldDefinitionCustomizer(graphqlFieldDefinitionCustomizer)).build(), fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();
        GraphQLObjectType subscriptionType = (GraphQLObjectType) schema.getType("Subscription");

        //Book Type
        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(TYPE_BOOK);
        assertEquals("The title of the book", bookType.getFieldDefinition("title").getDescription());
        assertEquals("The genre of the book", bookType.getFieldDefinition("genre").getDescription());
        assertEquals("The previews of the book", bookType.getFieldDefinition("previews").getDescription());
        assertEquals("The authors of the book", bookType.getFieldDefinition("authors").getDescription());

        GraphQLFieldDefinition bookField = subscriptionType.getFieldDefinition(BOOK);
        GraphQLEnumType bookTopicType = (GraphQLEnumType) bookField.getArgument(TOPIC).getType();
        assertEquals("BookTopic", bookTopicType.getName());
        assertNotNull(bookTopicType.getValue("ADDED"));
        assertNotNull(bookTopicType.getValue("UPDATED"));
        assertNull(bookTopicType.getValue("DELETED"));

        assertEquals(bookType, subscriptionType.getFieldDefinition(BOOK).getType());
        assertNotNull(subscriptionType.getFieldDefinition(BOOK).getArgument(FILTER));

        //Author Type
        GraphQLObjectType authorType = (GraphQLObjectType) schema.getType(TYPE_AUTHOR);
        assertEquals(authorType, subscriptionType.getFieldDefinition(AUTHOR).getType());
        assertNotNull(subscriptionType.getFieldDefinition(AUTHOR).getArgument(FILTER));

        GraphQLFieldDefinition authorField = subscriptionType.getFieldDefinition(AUTHOR);
        GraphQLEnumType authorTopicType = (GraphQLEnumType) authorField.getArgument(TOPIC).getType();
        assertEquals("AuthorTopic", authorTopicType.getName());
        assertNotNull(authorTopicType.getValue("ADDED"));
        assertNotNull(authorTopicType.getValue("UPDATED"));
        assertNotNull(authorTopicType.getValue("DELETED"));

        //Publisher Type
        assertNull(subscriptionType.getFieldDefinition("publisher"));

        //Preview Type (Custom Subscription)
        GraphQLObjectType previewType = (GraphQLObjectType) schema.getType(TYPE_PREVIEW);
        GraphQLFieldDefinition previewField = subscriptionType.getFieldDefinition(PREVIEW);
        assertEquals(previewType, previewField.getType());
        assertNull(previewField.getArgument(TOPIC));
    }

    @Test
    public void testModelTypes() {
        DataFetcher<?> fetcher = mock(DataFetcher.class);
        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary,
                new NonEntityDictionary(new DefaultClassScanner(), CoerceUtil::lookup), ElideSettings.builder().build(),
                fetcher, NO_VERSION);

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
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID, previewType.getFieldDefinition(FIELD_ID).getType());
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
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID, authorType.getFieldDefinition(FIELD_ID).getType());
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
        assertEquals(GraphQLScalars.GRAPHQL_DEFERRED_ID, previewType.getFieldDefinition(FIELD_ID).getType());
        assertEquals(2, previewType.getFieldDefinitions().size());

    }
}
