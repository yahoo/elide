/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.books.Author;
import com.yahoo.books.Book;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryTransaction;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
public class AbstractPersistentResourceFetcherTest extends AbstractGraphQLTest {
    protected GraphQL api;
    protected RequestScope requestScope;
    protected ObjectMapper mapper = new ObjectMapper();

    @BeforeMethod
    public void setup() {
        InMemoryDataStore store = new InMemoryDataStore(Author.class.getPackage());
        store.populateEntityDictionary(dictionary);

        ModelBuilder builder = new ModelBuilder(dictionary, new PersistentResourceFetcher());
        api = new GraphQL(builder.build());

        InMemoryTransaction tx = (InMemoryTransaction) store.beginTransaction();
        initTestData(tx);

        requestScope = new RequestScope("/", null, tx, null, null,
                new ElideSettingsBuilder(null)
                        .withEntityDictionary(dictionary)
                        .build());
    }

    private void initTestData(InMemoryTransaction tx) {
        Author author1 = new Author();
        author1.setId(1L);
        author1.setName("The People's Author");
        author1.setType(Author.AuthorType.EXCLUSIVE);

        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Libro Uno");
        book1.setAuthors(Collections.singletonList(author1));

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Libro Dos");
        book2.setAuthors(Collections.singletonList(author1));

        author1.setBooks(Arrays.asList(book1, book2));

        tx.save(author1, null);
        tx.save(book1, null);
        tx.save(book2, null);
        tx.commit(null);
    }

    protected void assertQueryEquals(String graphQLRequest, String expectedResponse) throws JsonProcessingException {
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        Assert.assertEquals(result.getErrors().size(), 0, "Errors [" + errorsToString(result.getErrors()) + "]:");
        Assert.assertEquals(mapper.writeValueAsString(result.getData()), expectedResponse);
    }

    void assertQueryFails(String graphQLRequest) {
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        Assert.assertNotEquals(result.getErrors().size(), 0);
    }

    protected String errorsToString(List<GraphQLError> errors) {
        return errors.stream()
                .map(GraphQLError::getMessage)
                .collect(Collectors.joining(", "));
    }
}
