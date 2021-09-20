/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.Elide;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.ExecutionResultSerializer;
import com.yahoo.elide.graphql.GraphQLErrorSerializer;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * JSR-356 Implementation of a web socket endpoint for GraphQL subscriptions.  JSR-356 should allow
 * cross-platform use for Spring, Quarkus, and other containers.
 */
@Slf4j
@ServerEndpoint(value = "/")
@Builder
public class SubscriptionWebSocket {
    private final ConcurrentMap<Session, SessionHandler> openSessions = new ConcurrentHashMap<>();
    private final DataStore topicStore;
    private final Elide elide;
    private final GraphQL api;

    @Builder.Default
    private int connectTimeoutMs = 5000;

    @Builder.Default
    private int maxSubscriptions = 30;

    @Builder.Default
    private UserFactory userFactory = DEFAULT_USER_FACTORY;

    @Builder.Default
    private String apiVersion = NO_VERSION;

    public static final UserFactory DEFAULT_USER_FACTORY = session -> new User(session.getUserPrincipal());

    /**
     * There is no standard for authentication for web sockets.  This interface delegates
     * Elide user creation to the developer.  This assumes authentication happens during the web socket handshake
     * and not after during message exchange.
     */
    @FunctionalInterface
    public interface UserFactory {
        User create(Session session);
    }

    /**
     * Constructor.
     * @param topicStore The JMS store.
     * @param elide Elide instance.
     * @param api Initialized GraphQL API for subscriptions.
     * @param connectTimeoutMs Connection timeout.
     * @param maxSubscriptions The maximum number of concurrent subscriptons per socket.
     * @param userFactory A function which creates an Elide user given a session object.
     */
    protected SubscriptionWebSocket(
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            int connectTimeoutMs,
            int maxSubscriptions,
            UserFactory userFactory,
            String apiVersion
    ) {
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.connectTimeoutMs = connectTimeoutMs;
        this.maxSubscriptions = maxSubscriptions;
        this.userFactory = userFactory;
        this.apiVersion = apiVersion;

        GraphQLErrorSerializer errorSerializer = new GraphQLErrorSerializer();
        SimpleModule module = new SimpleModule("ExecutionResultSerializer", Version.unknownVersion());
        module.addSerializer(ExecutionResult.class, new ExecutionResultSerializer(errorSerializer));
        module.addSerializer(GraphQLError.class, errorSerializer);
        elide.getElideSettings().getMapper().getObjectMapper().registerModule(module);
    }

    /**
     * Called on session open.
     * @param session The platform specific session object.
     * @throws IOException If there is an underlying error.
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {
        SessionHandler subscriptionSession = createSessionHandler(session);

        openSessions.put(session, subscriptionSession);
    }

    /**
     * Called on a new web socket message.
     * @param session The platform specific session object.
     * @param message The new message.
     * @throws IOException If there is an underlying error.
     */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        findSession(session).handleRequest(message);
    }

    /**
     * Called on session close.
     * @param session The platform specific session object.
     * @throws IOException If there is an underlying error.
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        findSession(session).safeClose(WebSocketCloseReasons.NORMAL_CLOSE);
        openSessions.remove(session);
    }

    /**
     * Called on a session error.
     * @param session The platform specific session object.
     * @param throwable The error that occurred.
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error(throwable.getMessage());
        findSession(session).safeClose(WebSocketCloseReasons.INTERNAL_ERROR);
        openSessions.remove(session);
    }

    private SessionHandler findSession(Session wrappedSession) {
        SessionHandler sessionHandler = openSessions.getOrDefault(wrappedSession, null);

        String message = "Unable to locate active session associated with: " + wrappedSession.toString();
        log.error(message);
        if (sessionHandler == null) {
            throw new IllegalStateException(message);
        }
        return sessionHandler;
    }

    protected SessionHandler createSessionHandler(Session session) {
        User user = userFactory.create(session);

        return new SessionHandler(session, topicStore, elide, api,
                connectTimeoutMs, maxSubscriptions,
                ConnectionInfo.builder()
                        .user(user)
                        .baseUrl(session.getRequestURI().getPath())
                        .parameters(session.getRequestParameterMap())
                        .getApiVersion(apiVersion)
                        .build());
    }
}
