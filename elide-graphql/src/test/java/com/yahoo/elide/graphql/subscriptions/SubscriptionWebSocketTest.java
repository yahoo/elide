/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.CONNECTION_TIMEOUT;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.INVALID_MESSAGE;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.MAX_SUBSCRIPTIONS;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.MULTIPLE_INIT;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.NORMAL_CLOSE;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.GraphQLTest;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Complete;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.ConnectionInit;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Subscribe;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons;
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
import javax.websocket.CloseReason;
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
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        endpoint.onClose(session);

        verify(remote, times(1)).sendText(message.capture());
        assertEquals("{\"type\":\"CONNECTION_ACK\"}", message.getAllValues().get(0));

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(NORMAL_CLOSE, closeReason.getValue());
    }

    @Test
    void testMissingType() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        String invalid = "{ \"id\": 123 }";
        endpoint.onOpen(session);
        endpoint.onMessage(session, invalid);

        verify(remote, never()).sendText(any());

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(INVALID_MESSAGE, closeReason.getValue());
    }

    @Test
    void testInvalidType() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        String invalid = "{ \"type\": \"foo\", \"id\": 123 }";
        endpoint.onOpen(session);
        endpoint.onMessage(session, invalid);

        verify(remote, never()).sendText(any());

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(INVALID_MESSAGE, closeReason.getValue());
    }

    @Test
    void testInvalidJson() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        //Missing payload field
        String invalid = "{ \"type\": \"SUBSCRIBE\"}";
        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));
        endpoint.onMessage(session, invalid);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(message.capture());
        assertEquals("{\"type\":\"CONNECTION_ACK\"}", message.getAllValues().get(0));

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(INVALID_MESSAGE, closeReason.getValue());
    }

    @Test
    void testConnectionTimeout() throws Exception {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .connectTimeoutMs(0).topicStore(dataStore).elide(elide).api(api).build();

        endpoint.onOpen(session);

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, timeout(1000).times(1)).close(closeReason.capture());
        assertEquals(CONNECTION_TIMEOUT, closeReason.getValue());
    }

    @Test
    void textMaxSubscriptions() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .maxSubscriptions(0).topicStore(dataStore).elide(elide).api(api).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));
        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .query("subscription {bookAdded {id title}}")
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(message.capture());
        assertEquals("{\"type\":\"CONNECTION_ACK\"}", message.getAllValues().get(0));

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(MAX_SUBSCRIPTIONS, closeReason.getValue());
    }

    @Test
    void testDoubleInit() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(message.capture());
        assertEquals("{\"type\":\"CONNECTION_ACK\"}", message.getAllValues().get(0));

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(MULTIPLE_INIT, closeReason.getValue());
    }

    @Test
    void testSubscribeBeforeInit() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        endpoint.onOpen(session);

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .query("subscription {bookAdded {id title}}")
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        verify(remote, never()).sendText(any());

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(UNAUTHORIZED, closeReason.getValue());
    }

    @Test
    void testDoubleSubscribe() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

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
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(2)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(WebSocketCloseReasons.CloseCode.DUPLICATE_ID.getCode(),
                closeReason.getValue().getCloseCode().getCode());
    }

    @Test
    void testSubscribeUnsubscribeSubscribe() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .query("subscription {bookAdded {id title}}")
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        Complete complete = Complete.builder().id("1").build();

        endpoint.onMessage(session, mapper.writeValueAsString(complete));
        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"CONNECTION_ACK\"}",
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}",
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testErrorInStream() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        reset(dataStoreTransaction);
        when(dataStoreTransaction.getAttribute(any(), any(), any())).thenThrow(new BadRequestException("Bad Request"));
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
                "{\"type\":\"NEXT\",\"id\":\"1\",\"payload\":{\"data\":{\"bookAdded\":{\"id\":\"1\",\"title\":null}},\"errors\":[{\"message\":\"Exception while fetching data (/bookAdded/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":29}],\"path\":[\"bookAdded\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}",
                "{\"type\":\"NEXT\",\"id\":\"1\",\"payload\":{\"data\":{\"bookAdded\":{\"id\":\"2\",\"title\":null}},\"errors\":[{\"message\":\"Exception while fetching data (/bookAdded/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":29}],\"path\":[\"bookAdded\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}",
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(4)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testErrorPriorToStream() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

        reset(dataStoreTransaction);
        when(dataStoreTransaction.loadObjects(any(), any())).thenThrow(new BadRequestException("Bad Request"));

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
                "{\"type\":\"NEXT\",\"id\":\"1\",\"payload\":{\"data\":null,\"errors\":[{\"message\":\"Exception while fetching data (/bookAdded) : Bad Request\",\"locations\":[{\"line\":1,\"column\":15}],\"path\":[\"bookAdded\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}",
                "{\"type\":\"COMPLETE\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testRootSubscription() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

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

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(4)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testSchemaQuery() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .topicStore(dataStore).elide(elide).api(api).build();

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

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }
}
