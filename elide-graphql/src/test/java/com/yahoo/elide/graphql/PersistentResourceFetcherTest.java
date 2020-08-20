/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import example.Author;
import example.Book;
import example.Price;
import example.Pseudonym;
import example.Publisher;

import org.apache.tools.ant.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public abstract class PersistentResourceFetcherTest extends GraphQLTest {
    protected GraphQL api;
    protected ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(GraphQL.class);
    private final String baseUrl = "http://localhost:8080/graphql";

    protected HashMapDataStore hashMapDataStore;
    protected InMemoryDataStore inMemoryDataStore;
    protected ElideSettings settings;

    public PersistentResourceFetcherTest() {
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        settings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        settings.getSerdes().forEach((targetType, serde) -> {
            CoerceUtil.register(targetType, serde);
        });

        hashMapDataStore = new HashMapDataStore(Author.class.getPackage());

        inMemoryDataStore = new InMemoryDataStore(
                new HashMapDataStore(Author.class.getPackage())
        );

        inMemoryDataStore.populateEntityDictionary(dictionary);
        NonEntityDictionary nonEntityDictionary = new NonEntityDictionary();
        ModelBuilder builder = new ModelBuilder(dictionary, nonEntityDictionary,
                new PersistentResourceFetcher(settings, nonEntityDictionary));

        api = new GraphQL(builder.build());

        initTestData();
    }

    @AfterEach
    public void clearTestData() {
        hashMapDataStore.cleanseTestData();
    }

    @BeforeEach
    public void initTestData() {
        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();

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
        book1.setPublicationDate(new Date(1514397817135L));
        book1.setPrice(new Price(new BigDecimal(123), Currency.getInstance("USD")));

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Libro Dos");
        book2.setAuthors(new ArrayList<>(Collections.singletonList(author1)));
        book2.setPublisher(publisher1);
        book2.setPublicationDate(new Date(0L));
        book2.setPrice(null);

        Price book2Price1 = new Price(new BigDecimal(200), Currency.getInstance("USD"));
        Price book2Price2 = new Price(new BigDecimal(210), Currency.getInstance("USD"));
        book2.setPriceHistory(Arrays.asList(
                book2Price1,
                book2Price2));

        book2.setPriceRevisions(new HashMap<>());
        book2.getPriceRevisions().put(new Date(1590187582000L), book2Price1);
        book2.getPriceRevisions().put(new Date(1590187682000L), book2Price2);

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
        book3.setPriceHistory(new ArrayList<>());

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

        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();
        RequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, settings);

        ExecutionResult result = api.execute(graphQLRequest, requestScope, variables);
        // NOTE: We're forcing commit even in case of failures. GraphQLEndpoint tests should ensure we do not commit on
        //       failure.
        if (isMutation) {
            requestScope.saveOrCreateObjects();
        }
        requestScope.getTransaction().commit(requestScope);
        assertEquals(0, result.getErrors().size(), "Errors [" + errorsToString(result.getErrors()) + "]:");
        try {
            LOG.info(mapper.writeValueAsString(result.getData()));
            assertEquals(
                    mapper.readTree(expectedResponse),
                    mapper.readTree(mapper.writeValueAsString(result.getData()))
            );
        } catch (JsonProcessingException e) {
            fail("JSON parsing exception", e);
        }
    }

    protected void assertQueryFailsWith(String graphQLRequest, String expectedMessage) throws Exception {
        boolean isMutation = graphQLRequest.startsWith("mutation");

        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();
        RequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, settings);

        ExecutionResult result = api.execute(graphQLRequest, requestScope);
        if (isMutation) {
            requestScope.saveOrCreateObjects();
        }
        requestScope.getTransaction().commit(requestScope);
        assertNotEquals(result.getErrors().size(), 0, "Expected errors. Received none.");
        try {
            String message = result.getErrors().get(0).getMessage();
            LOG.info(mapper.writeValueAsString(result.getErrors()));
            assertEquals(expectedMessage, message);
        } catch (JsonProcessingException e) {
            fail("JSON parsing exception", e);
        }
    }

    protected void assertQueryFails(String graphQLRequest) {
        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();
        RequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, settings);

        ExecutionResult result = api.execute(graphQLRequest, requestScope);

        //debug for errors
        LOG.debug("Errors = [" + errorsToString(result.getErrors()) + "]");

        assertNotEquals(result.getErrors().size(), 0);
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
