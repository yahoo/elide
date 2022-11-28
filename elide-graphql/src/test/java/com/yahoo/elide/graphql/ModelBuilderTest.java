/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static graphql.Assert.assertNotNull;
import static graphql.scalars.ExtendedScalars.GraphQLBigInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.dictionary.ArgumentType;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import example.Address;
import example.Author;
import example.Book;
import example.Publisher;
import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.scalars.java.JavaPrimitives;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import java.util.Arrays;
import java.util.HashSet;
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

    private static final String TYPE_QUERY = "Query";
    private static final String TYPE_BOOK_CONNECTION = "BookConnection";
    private static final String TYPE_BOOK_INPUT = "BookInput";
    private static final String TYPE_BOOK = "Book";
    private static final String TYPE_AUTHOR_CONNECTION = "AuthorConnection";
    private static final String TYPE_AUTHOR_INPUT = "AuthorInput";

    private static final String FIELD_BOOK = "book";
    private static final String FIELD_BOOKS = "books";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_AUTHORS = "authors";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_PUBLISH_DATE = "publishDate";
    private static final String FIELD_GENRE = "genre";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_WEIGHT_LBS = "weightLbs";
    private static final String FIELD_PUBLISHER = "publisher";

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
        dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(com.yahoo.elide.models.Book.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(Address.class);
    }

    @Test
    public void testInternalModelConflict() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ElideSettings settings = mock(ElideSettings.class);
        ModelBuilder builder = new ModelBuilder(dictionary,
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup),
                settings, fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();

        GraphQLType internalBookConnection = schema.getType("ElideInternalBookConnection");
        assertNotNull(internalBookConnection);

        GraphQLType internalBook = schema.getType("ElideInternalBook");
        assertNotNull(internalBook);

        GraphQLType internalBookEdge = schema.getType("ElideInternalBookEdge");
        assertNotNull(internalBookEdge);

        GraphQLType internalBookInput = schema.getType("ElideInternalBookInput");
        assertNotNull(internalBookInput);
    }


    @Test
    public void testPageInfoObject() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ElideSettings settings = mock(ElideSettings.class);
        ModelBuilder builder = new ModelBuilder(dictionary,
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup),
                settings, fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();

        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(TYPE_BOOK_CONNECTION);
        assertNotNull(bookType.getFieldDefinition(PAGE_INFO));
    }

    @Test
    public void testRelationshipParameters() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ElideSettings settings = mock(ElideSettings.class);
        ModelBuilder builder = new ModelBuilder(dictionary,
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup),
                settings, fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();
        GraphQLObjectType root = schema.getQueryType();
        assertNotNull(root);
        assertNotNull(root.getFieldDefinition(FIELD_BOOK));

        /* The root 'book' should have all query parameters defined */
        GraphQLFieldDefinition bookField = root.getFieldDefinition(FIELD_BOOK);
        assertNotNull(bookField.getArgument(DATA));
        assertNotNull(bookField.getArgument(FILTER));
        assertNotNull(bookField.getArgument(SORT));
        assertNotNull(bookField.getArgument(FIRST));
        assertNotNull(bookField.getArgument(AFTER));

        /* book.publisher is a 'to one' relationship so it should be missing all but the data parameter */
        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(TYPE_BOOK);
        GraphQLFieldDefinition publisherField = bookType.getFieldDefinition(FIELD_PUBLISHER);
        assertNotNull(publisherField.getArgument(DATA));
        assertNull(publisherField.getArgument(FILTER));
        assertNull(publisherField.getArgument(SORT));
        assertNull(publisherField.getArgument(FIRST));
        assertNull(publisherField.getArgument(AFTER));

        /* book.authors is a 'to many' relationship so it should have all query parameters defined */
        GraphQLFieldDefinition authorField = bookType.getFieldDefinition(FIELD_AUTHORS);
        assertNotNull(authorField.getArgument(DATA));
        assertNotNull(authorField.getArgument(FILTER));
        assertNotNull(authorField.getArgument(SORT));
        assertNotNull(authorField.getArgument(FIRST));
        assertNotNull(authorField.getArgument(AFTER));
    }

    @Test
    public void testBuild() {
        DataFetcher fetcher = mock(DataFetcher.class);
        ElideSettings settings = mock(ElideSettings.class);
        ModelBuilder builder = new ModelBuilder(dictionary,
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup),
                settings, fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();

        assertNotNull(schema.getType(TYPE_AUTHOR_CONNECTION));
        assertNotNull(schema.getType(TYPE_BOOK_CONNECTION));
        assertNotNull(schema.getType(TYPE_AUTHOR_INPUT));
        assertNotNull(schema.getType(TYPE_BOOK_INPUT));
        assertNotNull(schema.getType(TYPE_QUERY));

        GraphQLObjectType bookType = getConnectedType((GraphQLObjectType) schema.getType(TYPE_BOOK_CONNECTION), null);
        GraphQLObjectType authorType = getConnectedType((GraphQLObjectType) schema.getType(TYPE_AUTHOR_CONNECTION), null);
        GraphQLObjectType publisherConnectionType = (GraphQLObjectType) bookType.getFieldDefinition(FIELD_PUBLISHER)
                .getType();
        GraphQLList publisherEdgesType = (GraphQLList) publisherConnectionType.getFieldDefinition(EDGES)
                .getType();

        GraphQLObjectType publisherType = (GraphQLObjectType) ((GraphQLObjectType) publisherEdgesType.getWrappedType())
                .getFieldDefinition(NODE)
                .getType();

        //Test root type description fields.
        assertEquals("A GraphQL Book", bookType.getDescription());
        assertNull(authorType.getDescription());

        //Test non-root type description fields.
        assertEquals("A book publisher", publisherType.getDescription());

        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_TITLE).getType());
        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_GENRE).getType());
        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_LANGUAGE).getType());
        assertEquals(GraphQLBigInteger, bookType.getFieldDefinition(FIELD_PUBLISH_DATE).getType());
        assertEquals(JavaPrimitives.GraphQLBigDecimal, bookType.getFieldDefinition(FIELD_WEIGHT_LBS).getType());

        GraphQLObjectType addressType = (GraphQLObjectType) authorType.getFieldDefinition("homeAddress").getType();
        assertEquals(Scalars.GraphQLString, addressType.getFieldDefinition("street1").getType());
        assertEquals(Scalars.GraphQLString, addressType.getFieldDefinition("street2").getType());

        GraphQLObjectType authorsType = (GraphQLObjectType) bookType.getFieldDefinition(FIELD_AUTHORS).getType();
        GraphQLObjectType authorsNodeType = getConnectedType(authorsType, null);

        assertEquals(authorType, authorsNodeType);

        assertEquals(Scalars.GraphQLString, authorType.getFieldDefinition(FIELD_NAME).getType());

        assertTrue(validateEnum(Author.AuthorType.class,
                (GraphQLEnumType) authorType.getFieldDefinition(TYPE).getType()));

        // Node type != connection type
        GraphQLObjectType booksNodeType = (GraphQLObjectType) authorType.getFieldDefinition(FIELD_BOOKS).getType();
        assertFalse(booksNodeType.equals(bookType));

        GraphQLInputObjectType bookInputType = (GraphQLInputObjectType) schema.getType(TYPE_BOOK_INPUT);
        GraphQLInputObjectType authorInputType = (GraphQLInputObjectType) schema.getType(TYPE_AUTHOR_INPUT);

        assertEquals(Scalars.GraphQLString, bookInputType.getField(FIELD_TITLE).getType());
        assertEquals(Scalars.GraphQLString, bookInputType.getField(FIELD_GENRE).getType());
        assertEquals(Scalars.GraphQLString, bookInputType.getField(FIELD_LANGUAGE).getType());
        assertEquals(GraphQLBigInteger, bookInputType.getField(FIELD_PUBLISH_DATE).getType());

        GraphQLList authorsInputType = (GraphQLList) bookInputType.getField(FIELD_AUTHORS).getType();
        assertEquals(authorInputType, authorsInputType.getWrappedType());

        assertEquals(Scalars.GraphQLString, authorInputType.getField(FIELD_NAME).getType());

        GraphQLList booksInputType = (GraphQLList) authorInputType.getField(FIELD_BOOKS).getType();
        assertEquals(bookInputType, booksInputType.getWrappedType());
    }

    @Test
    public void checkAttributeArguments() {
        Set<ArgumentType> arguments = new HashSet<>();
        arguments.add(new ArgumentType(SORT, ClassType.of(Sorting.SortOrder.class)));
        arguments.add(new ArgumentType(TYPE, ClassType.STRING_TYPE));
        dictionary.addArgumentsToAttribute(ClassType.of(Book.class), FIELD_PUBLISH_DATE, arguments);

        DataFetcher fetcher = mock(DataFetcher.class);
        ElideSettings settings = mock(ElideSettings.class);
        ModelBuilder builder = new ModelBuilder(dictionary,
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup),
                settings, fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();

        GraphQLObjectType bookType = getConnectedType((GraphQLObjectType) schema.getType(TYPE_BOOK_CONNECTION), null);
        assertEquals(2, bookType.getFieldDefinition(FIELD_PUBLISH_DATE).getArguments().size());
        assertTrue(bookType.getFieldDefinition(FIELD_PUBLISH_DATE).getArgument(SORT).getType() instanceof GraphQLEnumType);
        assertEquals(Scalars.GraphQLString, bookType.getFieldDefinition(FIELD_PUBLISH_DATE).getArgument(TYPE).getType());
    }

    @Test
    public void checkModelArguments() {
        // Add test arguments to entities
        dictionary.addArgumentToEntity(ClassType.of(Book.class), new ArgumentType("filterBook", ClassType.STRING_TYPE));
        dictionary.addArgumentToEntity(ClassType.of(Publisher.class), new ArgumentType("filterPublisher", ClassType.STRING_TYPE));
        dictionary.addArgumentToEntity(ClassType.of(Author.class), new ArgumentType("filterAuthor", ClassType.STRING_TYPE));

        DataFetcher fetcher = mock(DataFetcher.class);
        ElideSettings settings = mock(ElideSettings.class);
        ModelBuilder builder = new ModelBuilder(dictionary,
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup),
                settings, fetcher, NO_VERSION);

        GraphQLSchema schema = builder.build();

        GraphQLObjectType root = schema.getQueryType();
        assertNotNull(root);
        assertNotNull(root.getFieldDefinition(FIELD_BOOK));

        /* The root 'book' should have the "filterBook" argument defined */
        GraphQLFieldDefinition bookField = root.getFieldDefinition(FIELD_BOOK);
        assertNotNull(bookField.getArgument("filterBook"));

        /* book.publisher is a "toOne" relationship and has the argument "filterPublisher" defined */
        GraphQLObjectType bookType = (GraphQLObjectType) schema.getType(TYPE_BOOK);
        GraphQLFieldDefinition publisherField = bookType.getFieldDefinition(FIELD_PUBLISHER);
        assertNotNull(publisherField.getArgument("filterPublisher"));

        /* book.authors is a 'to many' relationship and has the argument "filterAuthor" defined */
        GraphQLFieldDefinition authorField = bookType.getFieldDefinition(FIELD_AUTHORS);
        assertNotNull(authorField.getArgument("filterAuthor"));
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

    public static boolean validateEnum(Class<?> expected, GraphQLEnumType actual) {
        Enum [] values = (Enum []) expected.getEnumConstants();
        Set<String> enumNames = actual.getValues().stream()
                .map(GraphQLEnumValueDefinition::getName)
                .collect(Collectors.toSet());

        return Arrays.stream(values).allMatch(value -> enumNames.contains(value.name()));
    }
}
