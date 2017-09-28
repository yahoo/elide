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
import com.yahoo.elide.books.Publisher;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryTransaction;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
public abstract class PersistentResourceFetcherTest extends GraphQLTest {
    protected GraphQL api;
    protected RequestScope requestScope;
    protected ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(GraphQL.class);

    @BeforeMethod
    public void setupFetcherTest() {
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        ElideSettings settings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .build();

        InMemoryDataStore store = new InMemoryDataStore(Author.class.getPackage());
        store.populateEntityDictionary(dictionary);

        ModelBuilder builder = new ModelBuilder(dictionary, new PersistentResourceFetcher(settings));
        api = new GraphQL(builder.build());

        InMemoryTransaction tx = (InMemoryTransaction) store.beginTransaction();
        initTestData(tx);

        requestScope = new RequestScope("/", null, tx, null, null,
                settings, false);
    }

    private void initTestData(InMemoryTransaction tx) {
        Publisher publisher1 = new Publisher();
        publisher1.setId(1L);
        publisher1.setName("The Guy");

        Publisher publisher2 = new Publisher();
        publisher2.setId(2L);
        publisher2.setName("The Other Guy");

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
        book1.setPublisher(publisher1);

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Libro Dos");
        book2.setAuthors(new ArrayList<>(Collections.singletonList(author1)));
        book2.setPublisher(publisher1);

        author1.setPenName(authorOne);
        author1.setBooks(new ArrayList<>(Arrays.asList(book1, book2)));
        authorOne.setAuthor(author1);

        Author author2 = new Author();
        author2.setId(2L);
        author2.setName("Boris Pasternak");
        author2.setType(Author.AuthorType.EXCLUSIVE);

        Book book3 = new Book();
        book3.setId(3L);
        book3.setTitle("Doctor Zhivago");
        book3.setAuthors(new ArrayList<>(Collections.singletonList(author2)));
        book3.setPublisher(publisher2);

        author2.setBooks(new ArrayList<>(Collections.singletonList(book3)));

        publisher1.setBooks(new HashSet<>(Arrays.asList(book1, book2)));
        publisher2.setBooks(new HashSet<>(Arrays.asList(book3)));

        tx.save(author1, null);
        tx.save(authorOne, null);
        tx.save(author2, null);
        tx.save(book1, null);
        tx.save(book2, null);
        tx.save(book3, null);
        tx.save(publisher1, null);
        tx.save(publisher2, null);
        tx.commit(null);
    }

    protected void assertQueryEquals(String graphQLRequest, String expectedResponse) {
        boolean isMutation = graphQLRequest.startsWith("mutation");

        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        if (isMutation) {
            requestScope.saveOrCreateObjects();
        }
        requestScope.getTransaction().commit(requestScope);
        Assert.assertEquals(result.getErrors().size(), 0, "Errors [" + errorsToString(result.getErrors()) + "]:");
        try {
            LOG.debug(mapper.writeValueAsString(result.getData()));
            Assert.assertEquals(mapper.writeValueAsString(result.getData()), expectedResponse);
        } catch (JsonProcessingException e) {
            Assert.fail("JSON parsing exception", e);
        }
    }

    protected void assertQueryFails(String graphQLRequest) {
        ExecutionResult result = api.execute(graphQLRequest, requestScope);

        //debug for errors
        LOG.debug("Errors = [" + errorsToString(result.getErrors()) + "]");

        Assert.assertNotEquals(result.getErrors().size(), 0);
    }

    protected String errorsToString(List<GraphQLError> errors) {
        return errors.stream()
                .map(GraphQLError::getMessage)
                .collect(Collectors.joining(", "));
    }
}
