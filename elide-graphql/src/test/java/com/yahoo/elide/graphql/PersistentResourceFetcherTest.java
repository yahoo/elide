/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import example.Author;
import example.Book;
import example.Pseudonym;
import example.Publisher;

import org.apache.tools.ant.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
public abstract class PersistentResourceFetcherTest extends GraphQLTest {
    protected GraphQL api;
    protected GraphQLRequestScope requestScope;
    protected ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(GraphQL.class);

    @BeforeMethod
    public void setupFetcherTest() {
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        ElideSettings settings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        settings.getSerdes().forEach((targetType, serde) -> {
            CoerceUtil.register(targetType, serde);
        });

        InMemoryDataStore store = new InMemoryDataStore(
                new HashMapDataStore(Author.class.getPackage())
        );
        store.populateEntityDictionary(dictionary);

        ModelBuilder builder = new ModelBuilder(dictionary, new PersistentResourceFetcher(settings));
        api = new GraphQL(builder.build());

        DataStoreTransaction tx = store.beginTransaction();
        initTestData(tx);

        requestScope = new GraphQLRequestScope(tx, null, settings);
    }

    private void initTestData(DataStoreTransaction tx) {
        Publisher publisher1 = new Publisher();
        publisher1.setId(1L);
        publisher1.setName("The Guy");

        Publisher publisher2 = new Publisher();
        publisher2.setId(2L);
        publisher2.setName("The Other Guy");

        Author author1 = Author.builder()
                .id(1L)
                .name("Mark Twain")
                .type(Author.AuthorType.EXCLUSIVE)
                .build();

        Pseudonym authorOne = new Pseudonym();
        authorOne.setId(1L);
        authorOne.setName("The People's Author");

        Book book1 = Book.builder()
                .id(1L)
                .title("Libro Uno")
                .authors(new ArrayList<>(Collections.singletonList(author1)))
                .publisher(publisher1)
                .publicationDate(new Date(1514397817135L))
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Libro Dos")
                .authors(new ArrayList<>(Collections.singletonList(author1)))
                .publisher(publisher1)
                .publicationDate(new Date(0L))
                .build();

        author1.setPenName(authorOne);
        author1.setBooks(new ArrayList<>(Arrays.asList(book1, book2)));
        authorOne.setAuthor(author1);

        Author author2 = Author.builder()
                .id(2L)
                .name("Boris Pasternak")
                .type(Author.AuthorType.EXCLUSIVE)
                .build();

        Book book3 = Book.builder()
                .id(3L)
                .title("Doctor Zhivago")
                .authors(new ArrayList<>(Collections.singletonList(author2)))
                .publisher(publisher2)
                .build();

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

    protected void assertQueryEquals(String graphQLRequest, String expectedResponse) throws Exception {
        assertQueryEquals(graphQLRequest, expectedResponse, Collections.EMPTY_MAP);
    }

    protected void assertQueryEquals(String graphQLRequest, String expectedResponse, Map<String, Object> variables) throws Exception {
        boolean isMutation = graphQLRequest.startsWith("mutation");

        ExecutionResult result = api.execute(graphQLRequest, requestScope, variables);
        // NOTE: We're forcing commit even in case of failures. GraphQLEndpoint tests should ensure we do not commit on
        //       failure.
        if (isMutation) {
            requestScope.saveOrCreateObjects();
        }
        requestScope.getTransaction().commit(requestScope);
        Assert.assertEquals(result.getErrors().size(), 0, "Errors [" + errorsToString(result.getErrors()) + "]:");
        try {
            LOG.info(mapper.writeValueAsString(result.getData()));
            Assert.assertEquals(mapper.readTree(mapper.writeValueAsString(result.getData())),
                    mapper.readTree(expectedResponse));
        } catch (JsonProcessingException e) {
            Assert.fail("JSON parsing exception", e);
        }
    }

    protected void assertQueryFailsWith(String graphQLRequest, String expectedMessage) throws Exception {
        boolean isMutation = graphQLRequest.startsWith("mutation");

        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        if (isMutation) {
            requestScope.saveOrCreateObjects();
        }
        requestScope.getTransaction().commit(requestScope);
        Assert.assertNotEquals(result.getErrors().size(), 0, "Expected errors. Received none.");
        try {
            String message = result.getErrors().get(0).getMessage();
            LOG.info(mapper.writeValueAsString(result.getErrors()));
            Assert.assertEquals(message, expectedMessage);
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

    public String loadGraphQLRequest(String fileName) throws IOException {
        try (InputStream in = PersistentResourceFetcherTest.class.getResourceAsStream("/graphql/requests/" + fileName)) {
            return FileUtils.readFully(new InputStreamReader(in));
        }
    }

    public String loadGraphQLResponse(String fileName) throws IOException {
        try (InputStream in = PersistentResourceFetcherTest.class.getResourceAsStream("/graphql/responses/" + fileName)) {
            return FileUtils.readFully(new InputStreamReader(in));
        }
    }

    public void runComparisonTest(String testName) throws Exception {
        runComparisonTest(testName, this::assertQueryEquals);
    }

    public void runComparisonTestWithVariables(String testName, Map<String, Object> variables) throws Exception {
        String graphQLRequest = loadGraphQLRequest(testName + ".graphql");
        String graphQLResponse = loadGraphQLResponse(testName + ".json");
        assertQueryEquals(graphQLRequest, graphQLResponse, variables);
    }

    public void runErrorComparisonTest(String testName, String expectedMessage) throws Exception {
        String graphQLRequest = loadGraphQLRequest(testName + ".graphql");
        assertQueryFailsWith(graphQLRequest, expectedMessage);
    }

    protected void runComparisonTest(String testName, EvaluationFunction evalFn) throws Exception {
        String graphQLRequest = loadGraphQLRequest(testName + ".graphql");
        String graphQLResponse = loadGraphQLResponse(testName + ".json");
        evalFn.evaluate(graphQLRequest, graphQLResponse);
    }

    @FunctionalInterface
    protected interface EvaluationFunction {
        void evaluate(String graphQLRequest, String graphQLResponse) throws Exception;
    }
}
