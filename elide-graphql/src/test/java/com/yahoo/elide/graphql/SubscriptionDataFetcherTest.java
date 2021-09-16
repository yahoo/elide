/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.SubscriptionEntityProjectionMaker;
import com.yahoo.elide.graphql.subscriptions.SubscriptionDataFetcher;
import com.yahoo.elide.graphql.subscriptions.SubscriptionModelBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        settings = new ElideSettingsBuilder(null)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        settings.getSerdes().forEach(CoerceUtil::register);

        dataStore = mock(DataStore.class);
        dataStoreTransaction = mock(DataStoreTransaction.class);


        NonEntityDictionary nonEntityDictionary =
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup);

        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary, nonEntityDictionary,
                new SubscriptionDataFetcher(nonEntityDictionary), NO_VERSION);

        api = GraphQL.newGraphQL(builder.build())
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
        when(dataStoreTransaction.getRelation(any(), any(), any(), any())).thenCallRealMethod();
    }

    @Test
    void testRootSubscription() {
        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any())).thenReturn(List.of(book1, book2));

        List<String> responses = List.of(
                "{\"bookAdded\":{\"id\":\"1\",\"title\":\"Book 1\"}}",
                "{\"bookAdded\":{\"id\":\"2\",\"title\":\"Book 2\"}}"
        );

        String graphQLRequest = "subscription {bookAdded {id title}}";

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

        when(dataStoreTransaction.loadObjects(any(), any())).thenReturn(List.of(book1, book2));

        List<String> responses = List.of(
                "{\"bookAdded\":{\"id\":\"1\",\"title\":\"Book 1\",\"authors\":[{\"name\":\"Jane Doe\"},{\"name\":null}]}}",
                "{\"bookAdded\":{\"id\":\"2\",\"title\":\"Book 2\",\"authors\":[]}}"
        );

        String graphQLRequest = "subscription {bookAdded {id title authors { name }}}";

        assertSubscriptionEquals(graphQLRequest, responses);
    }

    protected void assertSubscriptionEquals(String graphQLRequest, List<String> expectedResponses) {
        List<ExecutionResult> results = runSubscription(graphQLRequest);

        assertEquals(expectedResponses.size(), results.size());

        for (int i = 0; i < expectedResponses.size(); i++) {
            String expectedResponse = expectedResponses.get(i);
            Object actualResponse = results.get(i).getData();

            try {
                LOG.info(mapper.writeValueAsString(actualResponse));
                assertEquals(
                        mapper.readTree(expectedResponse),
                        mapper.readTree(mapper.writeValueAsString(actualResponse))
                );
            } catch (JsonProcessingException e) {
                fail("JSON parsing exception", e);
            }
        }
    }

    protected List<ExecutionResult> runSubscription(String request) {
        DataStoreTransaction tx = dataStore.beginTransaction();
        GraphQLProjectionInfo projectionInfo =
                new SubscriptionEntityProjectionMaker(settings, new HashMap<>(), NO_VERSION).make(request);
        GraphQLRequestScope requestScope = new GraphQLRequestScope(baseUrl, tx, null, NO_VERSION, settings, projectionInfo, UUID.randomUUID(), null);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(request)
                .localContext(requestScope)
                .build();

        ExecutionResult executionResult = api.execute(executionInput);
        Publisher<ExecutionResult> resultPublisher = executionResult.getData();

        requestScope.getTransaction().commit(requestScope);

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
