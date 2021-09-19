/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
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
import com.yahoo.elide.graphql.GraphQLTest;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionEndpoint;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.ConnectionInit;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Subscribe;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    protected DataStore dataStore;
    protected DataStoreTransaction dataStoreTransaction;
    protected ElideSettings settings;
    protected Session session;
    protected RemoteEndpoint.Basic remote;
    protected Elide elide;

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

        elide = new Elide(settings);

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
    void testConnectionSetupAndTeardown() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        endpoint.onClose(session);

        verify(remote, times(2)).sendText(argumentCaptor.capture());
        assertEquals("{\"type\":\"CONNECTION_ACK\"}", argumentCaptor.getAllValues().get(0));
        assertEquals("1000: Normal Closure", argumentCaptor.getAllValues().get(1));
    }

    @Test
    void testMissingType() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        String invalid = "{ \"id\": 123 }";
        endpoint.onOpen(session);
        endpoint.onMessage(session, invalid);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(argumentCaptor.capture()); assertEquals("4400: Missing type field", argumentCaptor.getAllValues().get(0));
    }

    @Test
    void testInvalidType() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        String invalid = "{ \"type\": \"foo\", \"id\": 123 }";
        endpoint.onOpen(session);
        endpoint.onMessage(session, invalid);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(argumentCaptor.capture());
        assertEquals("4400: Unknown protocol message type 'foo'", argumentCaptor.getAllValues().get(0));
    }

    @Test
    void testInvalidJson() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        //Missing payload field
        String invalid = "{ \"type\": \"SUBSCRIBE\"}";
        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));
        endpoint.onMessage(session, invalid);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(remote, times(2)).sendText(argumentCaptor.capture());
        assertEquals("{\"type\":\"CONNECTION_ACK\"}", argumentCaptor.getAllValues().get(0));
        assertEquals("4400: Invalid message body", argumentCaptor.getAllValues().get(1));
    }

    @Test
    void testConnectionTimeout() throws Exception {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api, 0);

        endpoint.onOpen(session);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(remote, timeout(1000).times(1)).sendText(argumentCaptor.capture());
        assertEquals("4408: Connection initialisation timeout", argumentCaptor.getAllValues().get(0));
    }

    @Test
    void testDoubleInit() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(remote, times(2)).sendText(argumentCaptor.capture());
        assertEquals("{\"type\":\"CONNECTION_ACK\"}", argumentCaptor.getAllValues().get(0));
        assertEquals("4429: Too many initialization requests", argumentCaptor.getAllValues().get(1));
    }

    @Test
    void testSubscribeBeforeInit() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        endpoint.onOpen(session);

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .query("subscription {bookAdded {id title}}")
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(argumentCaptor.capture());
        assertEquals("4401: Unauthorized", argumentCaptor.getAllValues().get(0));
    }

    @Test
    void testDoubleSubscribe() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .query("subscription {bookAdded {id title}}")
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));
        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"CONNECTION_ACK\"}",
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}",
                "4409: Subscriber for 1 already exists"
        );

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(argumentCaptor.capture());
        assertEquals(expected, argumentCaptor.getAllValues());
    }

    @Test
    void testRootSubscription() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any())).thenReturn(List.of(book1, book2));

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .query("subscription {bookAdded {id title}}")
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"CONNECTION_ACK\"}",
                "{\"type\":\"NEXT\",\"id\":\"1\",\"payload\":{\"data\":{\"bookAdded\":{\"id\":\"1\",\"title\":\"Book 1\"}}}}",
                "{\"type\":\"NEXT\",\"id\":\"1\",\"payload\":{\"data\":{\"bookAdded\":{\"id\":\"2\",\"title\":\"Book 2\"}}}}",
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(remote, times(4)).sendText(argumentCaptor.capture());
        assertEquals(expected, argumentCaptor.getAllValues());
    }

    @Test
    void testSchemaQuery() throws IOException {
        SubscriptionEndpoint endpoint = new SubscriptionEndpoint(dataStore, elide, api);

        String graphQLRequest =
                "{"
                        + "__schema {"
                        + "types {"
                        + "   name"
                        + "}"
                        + "}"
                        + "__type(name: \"Author\") {"
                        + "   name"
                        + "   fields {"
                        + "     name"
                        + "     type { name }"
                        + "   }"
                        + "}"
                        + "}";

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .query(graphQLRequest)
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"CONNECTION_ACK\"}",
                "{\"type\":\"NEXT\",\"id\":\"1\",\"payload\":{\"data\":{\"__schema\":{\"types\":[{\"name\":\"Author\"},{\"name\":\"AuthorType\"},{\"name\":\"Book\"},{\"name\":\"Boolean\"},{\"name\":\"DeferredID\"},{\"name\":\"String\"},{\"name\":\"Subscription\"},{\"name\":\"__Directive\"},{\"name\":\"__DirectiveLocation\"},{\"name\":\"__EnumValue\"},{\"name\":\"__Field\"},{\"name\":\"__InputValue\"},{\"name\":\"__Schema\"},{\"name\":\"__Type\"},{\"name\":\"__TypeKind\"},{\"name\":\"address\"}]},\"__type\":{\"name\":\"Author\",\"fields\":[{\"name\":\"id\",\"type\":{\"name\":\"DeferredID\"}},{\"name\":\"homeAddress\",\"type\":{\"name\":\"address\"}},{\"name\":\"name\",\"type\":{\"name\":\"String\"}},{\"name\":\"type\",\"type\":{\"name\":\"AuthorType\"}}]}}}}",
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(argumentCaptor.capture());
        assertEquals(expected, argumentCaptor.getAllValues());
    }
}
