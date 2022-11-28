/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.dictionary.ArgumentType;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.GraphQLTest;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.SubscriptionEntityProjectionMaker;
import com.yahoo.elide.graphql.subscriptions.hooks.TopicType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Address;
import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class SubscriptionDataFetcherTest extends GraphQLTest {
    protected GraphQL api;
    protected ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(GraphQL.class);
    private final String baseUrl = "http://localhost:8080/graphql";

    protected DataStore dataStore;
    protected DataStoreTransaction dataStoreTransaction;
    protected ElideSettings settings;

    public SubscriptionDataFetcherTest() {
        RSQLFilterDialect filterDialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

        dataStore = mock(DataStore.class);
        dataStoreTransaction = mock(DataStoreTransaction.class);

        //This will be done by the JMS data store.
        dictionary.addArgumentToEntity(ClassType.of(Book.class), ArgumentType
                .builder()
                        .name("topic")
                        .type(ClassType.of(TopicType.class))
                .build());

        dictionary.addArgumentToEntity(ClassType.of(Author.class), ArgumentType
                .builder()
                .name("topic")
                .type(ClassType.of(TopicType.class))
                .build());

        settings = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        settings.getSerdes().forEach(CoerceUtil::register);

        NonEntityDictionary nonEntityDictionary =
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup);

        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary, nonEntityDictionary,
                new SubscriptionDataFetcher(nonEntityDictionary), NO_VERSION);

        api = GraphQL.newGraphQL(builder.build())
                .queryExecutionStrategy(new AsyncSerialExecutionStrategy())
                .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                .build();
    }

    @BeforeEach
    public void resetMocks() {
        reset(dataStore);
        reset(dataStoreTransaction);
        when(dataStore.beginTransaction()).thenReturn(dataStoreTransaction);
        when(dataStore.beginReadTransaction()).thenReturn(dataStoreTransaction);
        when(dataStoreTransaction.getAttribute(any(), any(), any())).thenCallRealMethod();
        when(dataStoreTransaction.getToManyRelation(any(), any(), any(), any())).thenCallRealMethod();
    }

    @Test
    void testRootSubscription() {
        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any()))
                .thenReturn(new DataStoreIterableBuilder(List.of(book1, book2)).build());

        List<String> responses = List.of(
                "{\"book\":{\"id\":\"1\",\"title\":\"Book 1\"}}",
                "{\"book\":{\"id\":\"2\",\"title\":\"Book 2\"}}"
        );

        String graphQLRequest = "subscription {book(topic: ADDED) {id title}}";

        assertSubscriptionEquals(graphQLRequest, responses);
    }

    @Test
    void testRootNonSchemaQuery() {
        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any()))
                .thenReturn(new DataStoreIterableBuilder(List.of(book1, book2)).build());

        List<String> responses = List.of("{\"book\":null}");
        List<String> errors = List.of("QUERY not supported for subscription models");

        String graphQLRequest = "query {book(topic: ADDED) {id title}}";

        assertSubscriptionEquals(graphQLRequest, responses, errors);
    }

    @Test
    void testRootSubscriptionWithFilter() {
        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any()))
                .thenReturn(new DataStoreIterableBuilder<>(List.of(book1, book2)).allInMemory().build());

        List<String> responses = List.of(
                "{\"book\":{\"id\":\"1\",\"title\":\"Book 1\"}}"
        );

        String graphQLRequest = "subscription {book(topic: ADDED, filter: \"title==*1*\") {id title}}";

        assertSubscriptionEquals(graphQLRequest, responses);
    }

    @Test
    void testComplexAttribute() {
        Author author1 = new Author();
        author1.setId(1L);
        author1.setHomeAddress(new Address());

        Author author2 = new Author();
        author2.setId(2L);
        Address address = new Address();
        address.setStreet1("123");
        address.setStreet2("XYZ");
        author2.setHomeAddress(address);

        when(dataStoreTransaction.loadObjects(any(), any()))
                .thenReturn(new DataStoreIterableBuilder(List.of(author1, author2)).build());

        List<String> responses = List.of(
                "{\"author\":{\"id\":\"1\",\"homeAddress\":{\"street1\":null,\"street2\":null}}}",
                "{\"author\":{\"id\":\"2\",\"homeAddress\":{\"street1\":\"123\",\"street2\":\"XYZ\"}}}"
        );

        String graphQLRequest = "subscription {author(topic: UPDATED) {id homeAddress { street1 street2 }}}";

        assertSubscriptionEquals(graphQLRequest, responses);
    }

    @Test
    void testRelationshipSubscription() {
        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);
        Author author1 = new Author();
        author1.setName("John Doe");

        Author author2 = new Author();
        author1.setName("Jane Doe");
        book1.setAuthors(List.of(author1, author2));

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any()))
                .thenReturn(new DataStoreIterableBuilder(List.of(book1, book2)).build());

        List<String> responses = List.of(
                "{\"bookAdded\":{\"id\":\"1\",\"title\":\"Book 1\",\"authors\":[{\"name\":\"Jane Doe\"},{\"name\":null}]}}",
                "{\"bookAdded\":{\"id\":\"2\",\"title\":\"Book 2\",\"authors\":[]}}"
        );

        String graphQLRequest = "subscription {bookAdded: book(topic:ADDED) {id title authors { name }}}";

        assertSubscriptionEquals(graphQLRequest, responses);
    }

    @Test
    void testSchemaSubscription() {
        String graphQLRequest =
                "{"
                        + "__schema {"
                        + "types {"
                        + "   name"
                        + "}"
                        + "}"
                        + "}";

        assertSubscriptionEquals(graphQLRequest, List.of("{\"__schema\":{\"types\":[{\"name\":\"Author\"},{\"name\":\"AuthorTopic\"},{\"name\":\"AuthorType\"},{\"name\":\"Book\"},{\"name\":\"BookTopic\"},{\"name\":\"Boolean\"},{\"name\":\"DeferredID\"},{\"name\":\"String\"},{\"name\":\"Subscription\"},{\"name\":\"__Directive\"},{\"name\":\"__DirectiveLocation\"},{\"name\":\"__EnumValue\"},{\"name\":\"__Field\"},{\"name\":\"__InputValue\"},{\"name\":\"__Schema\"},{\"name\":\"__Type\"},{\"name\":\"__TypeKind\"},{\"name\":\"address\"}]}}\n"));
    }

    @Test
    void testErrorInSubscriptionStream() {
        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        reset(dataStoreTransaction);
        when(dataStoreTransaction.getAttribute(any(), any(), any())).thenThrow(new BadRequestException("Bad Request"));
        when(dataStoreTransaction.loadObjects(any(), any()))
                .thenReturn(new DataStoreIterableBuilder(List.of(book1, book2)).build());

        List<String> responses = List.of(
                "{\"book\":{\"id\":\"1\",\"title\":null}}",
                "{\"book\":{\"id\":\"2\",\"title\":null}}"
        );

        List<String> errors = List.of("Bad Request", "Bad Request");

        String graphQLRequest = "subscription {book(topic: ADDED) {id title}}";

        assertSubscriptionEquals(graphQLRequest, responses, errors);
    }

    @Test
    void testErrorBeforeStream() {
        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any())).thenThrow(new BadRequestException("Bad Request"));

        List<String> responses = List.of("null");
        List<String> errors = List.of("Bad Request");

        String graphQLRequest = "subscription {book(topic:ADDED) {id title}}";

        assertSubscriptionEquals(graphQLRequest, responses, errors);
    }

    protected void assertSubscriptionEquals(String graphQLRequest, List<String> expectedResponses) {
        assertSubscriptionEquals(graphQLRequest, expectedResponses, new ArrayList<>());
    }

    protected void assertSubscriptionEquals(
            String graphQLRequest,
            List<String> expectedResponses,
            List<String> expectedErrors) {
            List<ExecutionResult> results = runSubscription(graphQLRequest);

        assertEquals(expectedResponses.size(), results.size());

        for (int i = 0; i < expectedResponses.size(); i++) {
            String expectedResponse = expectedResponses.get(i);
            String expectedError = "[]";
            if (!expectedErrors.isEmpty()) {
                    expectedError = expectedErrors.get(i);
            }
            ExecutionResult actualResponse = results.get(i);

            try {
                LOG.info(mapper.writeValueAsString(actualResponse));
                assertEquals(
                        mapper.readTree(expectedResponse),
                        mapper.readTree(mapper.writeValueAsString(actualResponse.getData()))
                );

                assertTrue(actualResponse.getErrors().toString().contains(expectedError));
            } catch (JsonProcessingException e) {
                fail("JSON parsing exception", e);
            }
        }
    }

    /**
     * Run a subscription.
     * @param request The subscription query.
     * @return A discrete list of results returned from the subscription.
     */
    protected List<ExecutionResult> runSubscription(String request) {
        InMemoryDataStore inMemoryDataStore = new InMemoryDataStore(dataStore);
        DataStoreTransaction tx = inMemoryDataStore.beginTransaction();

        GraphQLProjectionInfo projectionInfo =
                new SubscriptionEntityProjectionMaker(settings, new HashMap<>(), NO_VERSION).make(request);
        GraphQLRequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, NO_VERSION, settings, projectionInfo, UUID.randomUUID(), null);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(request)
                .localContext(requestScope)
                .build();

        ExecutionResult executionResult = api.execute(executionInput);

        if (! (executionResult.getData() instanceof Publisher)) {
            return List.of(executionResult);
        }

        Publisher<ExecutionResult> resultPublisher = executionResult.getData();

        requestScope.getTransaction().commit(requestScope);

        if (resultPublisher == null) {
            return List.of(executionResult);
        }

        List<ExecutionResult> results = new ArrayList<>();
        AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        resultPublisher.subscribe(new Subscriber<ExecutionResult>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscriptionRef.set(subscription);
                subscription.request(1);
            }

            @Override
            public void onNext(ExecutionResult executionResult) {
                results.add(executionResult);
                subscriptionRef.get().request(1);
            }

            @Override
            public void onError(Throwable t) {
                errorRef.set(t);
            }

            @Override
            public void onComplete() {
                //NOOP
            }
        });

        return results;
    }
}
