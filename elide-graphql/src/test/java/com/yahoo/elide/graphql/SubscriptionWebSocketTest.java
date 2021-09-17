/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.subscriptions.SubscriptionDataFetcher;
import com.yahoo.elide.graphql.subscriptions.SubscriptionModelBuilder;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.Address;
import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import graphql.GraphQL;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class SubscriptionWebSocketTest extends GraphQLTest {
    protected GraphQL api;
    protected ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(GraphQL.class);
    private final String baseUrl = "http://localhost:8080/graphql";

    protected DataStore dataStore;
    protected DataStoreTransaction dataStoreTransaction;
    protected ElideSettings settings;
    protected Session session;
    protected RemoteEndpoint.Basic remote;
    protected SubscriptionEndpoint endpoint;

    public SubscriptionWebSocketTest() {
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);
        dataStore = mock(DataStore.class);
        dataStoreTransaction = mock(DataStoreTransaction.class);
        session = mock(Session.class);
        remote = mock(RemoteEndpoint.Basic.class);

        settings = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(filterDialect)
                .withSubqueryFilterDialect(filterDialect)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .build();

        Elide elide = new Elide(settings);

        NonEntityDictionary nonEntityDictionary =
                new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup);

        SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary, nonEntityDictionary,
                new SubscriptionDataFetcher(nonEntityDictionary), NO_VERSION);

        api = GraphQL.newGraphQL(builder.build())
                .queryExecutionStrategy(new AsyncSerialExecutionStrategy())
                .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                .build();

        endpoint = new SubscriptionEndpoint(dataStore, elide, api);
    }

    @BeforeEach
    public void resetMocks() throws Exception {
        reset(dataStore);
        reset(dataStoreTransaction);
        reset(session);
        when(session.getRequestURI()).thenReturn(new URI("http://localhost:1234/subscription"));
        when(session.getBasicRemote()).thenReturn(remote);
        when(dataStore.beginTransaction()).thenReturn(dataStoreTransaction);
        when(dataStore.beginReadTransaction()).thenReturn(dataStoreTransaction);
        when(dataStoreTransaction.getAttribute(any(), any(), any())).thenCallRealMethod();
        when(dataStoreTransaction.getRelation(any(), any(), any(), any())).thenCallRealMethod();
    }

    @Test
    void testRootSubscription() throws IOException {
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
    void testComplexAttribute() throws IOException {
        Author author1 = new Author();
        author1.setId(1L);
        author1.setHomeAddress(new Address());

        Author author2 = new Author();
        author2.setId(2L);
        Address address = new Address();
        address.setStreet1("123");
        address.setStreet2("XYZ");
        author2.setHomeAddress(address);

        when(dataStoreTransaction.loadObjects(any(), any())).thenReturn(List.of(author1, author2));

        List<String> responses = List.of(
                "{\"authorUpdated\":{\"id\":\"1\",\"homeAddress\":{\"street1\":null,\"street2\":null}}}",
                "{\"authorUpdated\":{\"id\":\"2\",\"homeAddress\":{\"street1\":\"123\",\"street2\":\"XYZ\"}}}"
        );

        String graphQLRequest = "subscription {authorUpdated {id homeAddress { street1 street2 }}}";

        assertSubscriptionEquals(graphQLRequest, responses);
    }

    @Test
    void testRelationshipSubscription() throws IOException {
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

    @Test
    void testSchemaSubscription() throws IOException {
        String graphQLRequest =
                "{"
                        + "__schema {"
                        + "types {"
                        + "   name"
                        + "}"
                        + "}"
                        + "}";

        assertSubscriptionEquals(graphQLRequest, List.of("{\"__schema\":{\"types\":[{\"name\":\"Author\"},{\"name\":\"AuthorType\"},{\"name\":\"Book\"},{\"name\":\"Boolean\"},{\"name\":\"DeferredID\"},{\"name\":\"String\"},{\"name\":\"Subscription\"},{\"name\":\"__Directive\"},{\"name\":\"__DirectiveLocation\"},{\"name\":\"__EnumValue\"},{\"name\":\"__Field\"},{\"name\":\"__InputValue\"},{\"name\":\"__Schema\"},{\"name\":\"__Type\"},{\"name\":\"__TypeKind\"},{\"name\":\"address\"}]}}"));
    }

    protected void assertSubscriptionEquals(String graphQLRequest, List<String> expectedResponses) throws IOException {
        assertSubscriptionEquals(graphQLRequest, expectedResponses, new ArrayList<>());
    }

    protected void assertSubscriptionEquals(
            String graphQLRequest,
            List<String> expectedResponses,
            List<String> expectedErrors) throws IOException {
        List<String> results = runSubscription(graphQLRequest, expectedResponses.size());

        assertEquals(expectedResponses.size(), results.size());

        for (int i = 0; i < expectedResponses.size(); i++) {
            String expectedResponse = expectedResponses.get(i);
            String actualResponse = results.get(i);

            LOG.info(mapper.writeValueAsString(actualResponse));
            assertEquals(expectedResponse, actualResponse);
        }
    }

    /**
     * Run a subscription
     * @param request The subscription query.
     * @return A discrete list of results returned from the subscription.
     */
    protected List<String> runSubscription(String request, int expectedResponseCount) throws IOException {
        endpoint.onOpen(session);
        endpoint.onMessage(session, request);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(remote, times(expectedResponseCount)).sendText(argumentCaptor.capture());

        return argumentCaptor.getAllValues();
    }
}
