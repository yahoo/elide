/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.subscriptions;

import static com.paiondata.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.CONNECTION_TIMEOUT;
import static com.paiondata.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.INVALID_MESSAGE;
import static com.paiondata.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.MULTIPLE_INIT;
import static com.paiondata.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.NORMAL_CLOSE;
import static com.paiondata.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreIterableBuilder;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.ArgumentType;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;
import com.paiondata.elide.graphql.GraphQLTest;
import com.paiondata.elide.graphql.serialization.GraphQLModule;
import com.paiondata.elide.graphql.subscriptions.hooks.TopicType;
import com.paiondata.elide.graphql.subscriptions.websocket.SubscriptionWebSocket;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.Complete;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.ConnectionInit;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.Subscribe;
import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.MoreExecutors;
import example.Author;
import example.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

/**
 * Base functionality required to test the PersistentResourceFetcher.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Slf4j
public class SubscriptionWebSocketTest extends GraphQLTest {
    protected ObjectMapper mapper = new ObjectMapper();

    protected DataStore dataStore;
    protected DataStoreTransaction dataStoreTransaction;
    protected ElideSettings settings;
    protected Session session;
    protected RemoteEndpoint.Async remote;
    protected Elide elide;
    protected ExecutorService executorService = MoreExecutors.newDirectExecutorService();
    protected DataFetcherExceptionHandler dataFetcherExceptionHandler = spy(new SimpleDataFetcherExceptionHandler());
    protected EndpointConfig endpointConfig;

    public SubscriptionWebSocketTest() {
        RSQLFilterDialect filterDialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

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

        dataStore = mock(DataStore.class);
        dataStoreTransaction = mock(DataStoreTransaction.class);
        session = mock(Session.class);
        remote = mock(RemoteEndpoint.Async.class);

        JsonApiSettingsBuilder jsonApiSettings = JsonApiSettingsBuilder.withDefaults(dictionary)
                .joinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build());
        GraphQLSettingsBuilder graphqlSettings = GraphQLSettingsBuilder.withDefaults(dictionary);

        settings = ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .settings(jsonApiSettings, graphqlSettings)
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")))
                .build();

        elide = new Elide(settings);

        elide.getObjectMapper().registerModule(new GraphQLModule());
    }

    @BeforeEach
    public void resetMocks() throws Exception {
        reset(dataStore);
        reset(dataStoreTransaction);
        reset(session);
        reset(dataFetcherExceptionHandler);
        when(session.getRequestURI()).thenReturn(new URI("http://localhost:1234/subscription"));
        when(session.getAsyncRemote()).thenReturn(remote);
        when(dataStore.beginTransaction()).thenReturn(dataStoreTransaction);
        when(dataStore.beginReadTransaction()).thenReturn(dataStoreTransaction);
        when(dataStoreTransaction.getAttribute(any(), any(), any())).thenCallRealMethod();
        when(dataStoreTransaction.getToManyRelation(any(), any(), any(), any())).thenCallRealMethod();
    }

    @Test
    void testConnectionSetupAndTeardown() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        endpoint.onClose(session, null);

        verify(remote, times(1)).sendText(message.capture());
        assertEquals("{\"type\":\"connection_ack\"}", message.getAllValues().get(0));

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(NORMAL_CLOSE, closeReason.getValue());
    }

    @Test
    void testMissingType() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        String invalid = "{ \"id\": 123 }";
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, invalid);

        verify(remote, never()).sendText(any());

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(INVALID_MESSAGE, closeReason.getValue());
    }

    @Test
    void testInvalidType() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        String invalid = "{ \"type\": \"foo\", \"id\": 123 }";
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, invalid);

        verify(remote, never()).sendText(any());

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(INVALID_MESSAGE, closeReason.getValue());
    }

    @Test
    void testInvalidJson() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        //Missing payload field
        String invalid = "{ \"type\": \"subscribe\"}";
        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));
        endpoint.onMessage(session, invalid);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(message.capture());
        assertEquals("{\"type\":\"connection_ack\"}", message.getAllValues().get(0));

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(INVALID_MESSAGE, closeReason.getValue());
    }

    @Test
    void testConnectionTimeout() throws Exception {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .connectionTimeout(Duration.ZERO).elide(elide).build();

        endpoint.onOpen(session, endpointConfig);

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, timeout(1000).times(1)).close(closeReason.capture());
        assertEquals(CONNECTION_TIMEOUT, closeReason.getValue());
    }

    @Test
    void testDoubleInit() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(remote, times(1)).sendText(message.capture());
        assertEquals("{\"type\":\"connection_ack\"}", message.getAllValues().get(0));

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(MULTIPLE_INIT, closeReason.getValue());
    }

    @Test
    void testSubscribeBeforeInit() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        endpoint.onOpen(session, endpointConfig);

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .payload(Subscribe.Payload.builder()
                        .query("subscription {book(topic: ADDED) {id title}}")
                        .build())
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        verify(remote, never()).sendText(any());

        ArgumentCaptor<CloseReason> closeReason = ArgumentCaptor.forClass(CloseReason.class);
        verify(session, times(1)).close(closeReason.capture());
        assertEquals(UNAUTHORIZED, closeReason.getValue());
    }

    @Test
    void testSubscribeUnsubscribeSubscribe() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .payload(Subscribe.Payload.builder()
                        .query("subscription {book(topic: ADDED) {id title}}")
                        .build())
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        Complete complete = Complete.builder().id("1").build();

        endpoint.onMessage(session, mapper.writeValueAsString(complete));
        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"connection_ack\"}",
                "{\"type\":\"complete\",\"id\":\"1\"}",
                "{\"type\":\"complete\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testErrorInStream() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide)
                .dataFetcherExceptionHandler(dataFetcherExceptionHandler)
                .build();

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

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .payload(Subscribe.Payload.builder()
                        .query("subscription {book(topic: ADDED) {id title}}")
                        .build())
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"connection_ack\"}",
                "{\"type\":\"next\",\"id\":\"1\",\"payload\":{\"data\":{\"book\":{\"id\":\"1\",\"title\":null}},\"errors\":[{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":38}],\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}",
                "{\"type\":\"next\",\"id\":\"1\",\"payload\":{\"data\":{\"book\":{\"id\":\"2\",\"title\":null}},\"errors\":[{\"message\":\"Exception while fetching data (/book/title) : Bad Request\",\"locations\":[{\"line\":1,\"column\":38}],\"path\":[\"book\",\"title\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}",
                "{\"type\":\"complete\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(4)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
        verify(dataFetcherExceptionHandler, times(2)).handleException(any());
    }

    @Test
    void testErrorPriorToStream() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        reset(dataStoreTransaction);
        when(dataStoreTransaction.loadObjects(any(), any())).thenThrow(new BadRequestException("Bad Request"));

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .payload(Subscribe.Payload.builder()
                        .query("subscription {book(topic: ADDED) {id title}}")
                        .build())
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"connection_ack\"}",
                "{\"type\":\"next\",\"id\":\"1\",\"payload\":{\"data\":null,\"errors\":[{\"message\":\"Exception while fetching data (/book) : Bad Request\",\"locations\":[{\"line\":1,\"column\":15}],\"path\":[\"book\"],\"extensions\":{\"classification\":\"DataFetchingException\"}}]}}",
                "{\"type\":\"complete\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testRootSubscription() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        Book book1 = new Book();
        book1.setTitle("Book 1");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Book 2");
        book2.setId(2);

        when(dataStoreTransaction.loadObjects(any(), any()))
                .thenReturn(new DataStoreIterableBuilder(List.of(book1, book2)).build());

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .payload(Subscribe.Payload.builder()
                        .query("subscription {book(topic: ADDED) {id title}}")
                        .build())
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"connection_ack\"}",
                "{\"type\":\"next\",\"id\":\"1\",\"payload\":{\"data\":{\"book\":{\"id\":\"1\",\"title\":\"Book 1\"}}}}",
                "{\"type\":\"next\",\"id\":\"1\",\"payload\":{\"data\":{\"book\":{\"id\":\"2\",\"title\":\"Book 2\"}}}}",
                "{\"type\":\"complete\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(4)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testSchemaQuery() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

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
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .payload(Subscribe.Payload.builder()
                        .query(graphQLRequest)
                        .build())
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        List<String> expected = List.of(
                "{\"type\":\"connection_ack\"}",
                "{\"type\":\"next\",\"id\":\"1\",\"payload\":{\"data\":{\"__schema\":{\"types\":[{\"name\":\"Author\"},{\"name\":\"AuthorTopic\"},{\"name\":\"AuthorType\"},{\"name\":\"Book\"},{\"name\":\"BookTopic\"},{\"name\":\"Boolean\"},{\"name\":\"DeferredID\"},{\"name\":\"String\"},{\"name\":\"Subscription\"},{\"name\":\"__Directive\"},{\"name\":\"__DirectiveLocation\"},{\"name\":\"__EnumValue\"},{\"name\":\"__Field\"},{\"name\":\"__InputValue\"},{\"name\":\"__Schema\"},{\"name\":\"__Type\"},{\"name\":\"__TypeKind\"},{\"name\":\"address\"}]},\"__type\":{\"name\":\"Author\",\"fields\":[{\"name\":\"id\",\"type\":{\"name\":\"DeferredID\"}},{\"name\":\"homeAddress\",\"type\":{\"name\":\"address\"}},{\"name\":\"name\",\"type\":{\"name\":\"String\"}},{\"name\":\"type\",\"type\":{\"name\":\"AuthorType\"}}]}}}}",
                "{\"type\":\"complete\",\"id\":\"1\"}"
        );

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(remote, times(3)).sendText(message.capture());
        assertEquals(expected, message.getAllValues());
    }

    @Test
    void testActualComplete() throws IOException {
        SubscriptionWebSocket endpoint = SubscriptionWebSocket.builder()
                .executorService(executorService)
                .elide(elide).build();

        ConnectionInit init = new ConnectionInit();
        endpoint.onOpen(session, endpointConfig);
        endpoint.onMessage(session, mapper.writeValueAsString(init));

        Subscribe subscribe = Subscribe.builder()
                .id("1")
                .payload(Subscribe.Payload.builder()
                        .query("subscription {book(topic: ADDED) {id title}}")
                        .build())
                .build();

        endpoint.onMessage(session, mapper.writeValueAsString(subscribe));

        String complete = "{\"id\":\"5d585eff-ed05-48c2-8af7-ad662930ba74\",\"type\":\"complete\"}";

        endpoint.onMessage(session, complete);
    }
}
