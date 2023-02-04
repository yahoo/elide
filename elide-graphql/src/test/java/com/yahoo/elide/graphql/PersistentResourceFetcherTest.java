/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import example.Author;
import example.Book;
import example.Price;
import example.Pseudonym;
import example.Publisher;
import org.apache.tools.ant.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PersistentResourceFetcherTest extends GraphQLTest {
    protected ObjectMapper mapper = new ObjectMapper();
    protected QueryRunner runner;
    private static final Logger LOG = LoggerFactory.getLogger(GraphQL.class);
    private final String baseUrl = "http://localhost:8080/graphql";
    protected User user = mock(User.class);

    protected HashMapDataStore hashMapDataStore;
    protected ElideSettings settings;

    @BeforeAll
    public void initializeQueryRunner() {
        RSQLFilterDialect filterDialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

        hashMapDataStore = new HashMapDataStore(DefaultClassScanner.getInstance(), Author.class.getPackage());

        settings = new ElideSettingsBuilder(hashMapDataStore)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .withGraphQLFederation(true)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        settings.getSerdes().forEach(CoerceUtil::register);

        initializeMocks();
        Elide elide = new Elide(settings);
        elide.doScans();

        runner = new QueryRunner(elide, NO_VERSION);
    }

    protected void initializeMocks() {
        //NOOP;
    }

    @AfterEach
    public void clearTestData() {
        hashMapDataStore.cleanseTestData();
    }

    @BeforeEach
    public void initTestData() {
        DataStoreTransaction tx = hashMapDataStore.beginTransaction();

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
        assertQueryEquals(graphQLRequest, expectedResponse, Collections.emptyMap());
    }

    protected void assertQueryEquals(String graphQLRequest, String expectedResponse, Map<String, Object> variables)
            throws Exception {
        ElideResponse response = runGraphQLRequest(graphQLRequest, variables);

        JsonNode data = mapper.readTree(response.getBody()).get("data");
        assertNotNull(data);

        assertEquals(
                mapper.readTree(expectedResponse),
                mapper.readTree(data.toString())
        );
    }

    protected void assertQueryFailsWith(String graphQLRequest, String expectedMessage) throws Exception {
        ElideResponse response = runGraphQLRequest(graphQLRequest, new HashMap<>());

        JsonNode errors = mapper.readTree(response.getBody()).get("errors");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
        JsonNode message = errors.get(0).get("message");
        assertNotNull(message);

        assertEquals('"' + expectedMessage + '"', message.toString());
    }

    protected void assertQueryFails(String graphQLRequest) throws IOException {
        ElideResponse result = runGraphQLRequest(graphQLRequest, new HashMap<>());

        assertTrue(result.getBody().contains("errors"));
    }

    protected void assertParsingFails(String graphQLRequest) {
        assertThrows(Exception.class, () -> new GraphQLEntityProjectionMaker(settings).make(graphQLRequest));
    }

    protected ElideResponse runGraphQLRequest(String graphQLRequest, Map<String, Object> variables)
            throws IOException {
        String requestWithEnvelope = toGraphQLQuery(graphQLRequest, variables);

        return runner.run(baseUrl, requestWithEnvelope, user);
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

    protected String toGraphQLQuery(String query, Map<String, Object> variables) throws IOException {
        ObjectNode graphqlNode = JsonNodeFactory.instance.objectNode();
        graphqlNode.put("query", query);
        if (variables != null) {
            graphqlNode.set("variables", mapper.valueToTree(variables));
        }
        return mapper.writeValueAsString(graphqlNode);
    }

    @FunctionalInterface
    protected interface EvaluationFunction {
        void evaluate(String graphQLRequest, String graphQLResponse) throws Exception;
    }
}
