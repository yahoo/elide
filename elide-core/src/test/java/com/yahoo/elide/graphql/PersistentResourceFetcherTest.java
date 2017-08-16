/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.books.Author;
import com.yahoo.elide.books.Book;
import com.yahoo.elide.books.Pseudonym;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryTransaction;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
public class PersistentResourceFetcherTest extends GraphQLTest {
    protected GraphQL api;
    protected RequestScope requestScope;
    protected ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(GraphQL.class);

    @BeforeMethod
    public void setupFetcherTest() {
        ElideSettings settings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary).build();

        InMemoryDataStore store = new InMemoryDataStore(Author.class.getPackage());
        store.populateEntityDictionary(dictionary);

        ModelBuilder builder = new ModelBuilder(dictionary, new PersistentResourceFetcher(settings));
        api = new GraphQL(builder.build());

        InMemoryTransaction tx = (InMemoryTransaction) store.beginTransaction();
        initTestData(tx);

        requestScope = new RequestScope("/", null, tx, null, null,
                settings);
    }

    private void initTestData(InMemoryTransaction tx) {
        Author author1 = new Author();
        author1.setId(1L);
        author1.setName("Mark Twain");
        author1.setType(Author.AuthorType.EXCLUSIVE);

        Pseudonym authorOne = new Pseudonym();
        authorOne.setId(1L);
        authorOne.setName("The People's Author");

        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Libro Uno");
        book1.setAuthors(new ArrayList<>(Collections.singletonList(author1)));

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Libro Dos");

        author1.setPenName(authorOne);
        author1.setBooks(new ArrayList<>(Arrays.asList(book1, book2)));
        authorOne.setAuthor(author1);

        tx.save(author1, null);
        tx.save(authorOne, null);
        tx.save(book1, null);
        tx.save(book2, null);
        tx.commit(null);
    }

    protected void assertQueryEquals(String graphQLRequest, String expectedResponse) {
        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        requestScope.getTransaction().commit(requestScope);
        Assert.assertEquals(result.getErrors().size(), 0, "Errors [" + errorsToString(result.getErrors()) + "]:");
        try {
            log.debug(mapper.writeValueAsString(result.getData()));
            Assert.assertEquals(mapper.writeValueAsString(result.getData()), expectedResponse);
        } catch (JsonProcessingException e) {
            Assert.fail("JSON parsing exception", e);
        }
    }

    protected void assertQueryFails(String graphQLRequest) {
        ExecutionResult result = api.execute(graphQLRequest, requestScope);

        //debug for errors
        log.debug("Errors = [" + errorsToString(result.getErrors()) + "]");

        Assert.assertNotEquals(result.getErrors().size(), 0);
    }

    protected String errorsToString(List<GraphQLError> errors) {
        return errors.stream()
                .map(GraphQLError::getMessage)
                .collect(Collectors.joining(", "));
    }
}
