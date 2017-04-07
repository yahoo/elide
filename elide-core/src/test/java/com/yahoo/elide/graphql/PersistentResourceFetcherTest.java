/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import example.Author;
import example.Book;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test our data fetcher.
 */
public class PersistentResourceFetcherTest extends AbstractGraphQLTest {
    protected GraphQL api;
    protected RequestScope requestScope;
    protected ObjectMapper mapper = new ObjectMapper();

    @BeforeMethod
    public void setup() {
        PersistentResourceFetcher fetcher = new PersistentResourceFetcher();
        ModelBuilder builder = new ModelBuilder(dictionary, fetcher);
        api = new GraphQL(builder.build());
        DataStoreTransaction tx = Mockito.mock(DataStoreTransaction.class);

        initTestData(tx);

        requestScope = new RequestScope("/", null, tx, null, null,
                new ElideSettingsBuilder(null)
                        .withEntityDictionary(dictionary)
                        .build());
    }

    public void initTestData(DataStoreTransaction tx) {
        Author author1 = new Author();
        author1.setId(1L);
        author1.setName("The People's Author");
        author1.setType(Author.AuthorType.EXCLUSIVE);

        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book Numero Uno");
        book1.setAuthors(Collections.singletonList(author1));

        when(tx.createNewObject(any())).thenCallRealMethod();
        when(tx.loadObject(eq(Book.class), eq(1L), any(), any())).thenReturn(book1);
        when(tx.loadObject(eq(Author.class), eq(1L), any(), any())).thenReturn(author1);
        when(tx.loadObjects(eq(Book.class), any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(book1)));
        when(tx.loadObjects(eq(Author.class), any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(author1)));
    }

    @Test
    public void testFetchRootObject() throws JsonProcessingException {
        String graphQLRequest = "{ book(id: \"1\") { id title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testFetchRootCollection() throws JsonProcessingException {
        String graphQLRequest = "{ book { id title genre language } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\",\"genre\":null,\"language\":null}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testFetchNestedCollection() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testFetchNestedObject() throws JsonProcessingException {
        String graphQLRequest = "{ author(id: \"1\") { books(id: \"1\") { id title } } }";
        String expectedResponse = "{\"author\":[{\"books\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\"}]}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }


    @Test
    public void testCreateRootObject() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op: ADD, relationship: { title: \"Book Numero Dos\" } ) { title } }";
        String expectedResponse = "{\"book\":[{\"title\":\"Book Numero Dos\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testUpdateRootObject() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op:REPLACE, id: \"1\", relationship: { title: \"abc\" }) { id, title } }";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"abc\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testDeleteRootObject() throws JsonProcessingException {
        String graphQLRequest = "mutation { book(op:DELETE, id: \"1\"){ id, title }}";
        String expectedResponse = "{\"book\":[{\"id\":\"1\",\"title\":\"Book Numero Uno\"}]}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    private void assertQueryEquals(String graphQLRequest, String expectedResponse) throws JsonProcessingException {
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        Assert.assertEquals(result.getErrors().size(), 0, errorsToString(result.getErrors()));
        Assert.assertEquals(mapper.writeValueAsString(result.getData()), expectedResponse);
    }

    private String errorsToString(List<GraphQLError> errors) {
        return errors.stream()
                .map(GraphQLError::getMessage)
                .collect(Collectors.joining(", "));
    }
}
