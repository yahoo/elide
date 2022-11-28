/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.graphql.NonEntityDictionary;
import com.yahoo.elide.graphql.subscriptions.SubscriptionDataFetcher;
import com.yahoo.elide.graphql.subscriptions.SubscriptionModelBuilder;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons;

import graphql.GraphQL;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

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
@ServerEndpoint(value = "/", subprotocols = { "graphql-transport-ws" })
@Builder
public class SubscriptionWebSocket {
    private Elide elide;
    private ExecutorService executorService;

    @Builder.Default
    private int connectTimeoutMs = 5000;

    @Builder.Default
    private int maxSubscriptions = 30;

    @Builder.Default
    private UserFactory userFactory = DEFAULT_USER_FACTORY;

    @Builder.Default
    private long maxIdleTimeoutMs = 300000;

    @Builder.Default
    private int maxMessageSize = 10000;

    @Builder.Default
    private boolean sendPingOnSubscribe = false;

    @Builder.Default
    private boolean verboseErrors = false;

    private final Map<String, GraphQL> apis = new HashMap<>();
    private final ConcurrentMap<Session, SessionHandler> openSessions = new ConcurrentHashMap<>();

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
     * @param elide Elide instance.
     * @param executorService Thread pool for all websockets. If null each session will make its own.
     * @param connectTimeoutMs Connection timeout.
     * @param maxSubscriptions The maximum number of concurrent subscriptions per socket.
     * @param userFactory A function which creates an Elide user given a session object.
     * @param maxIdleTimeoutMs Max idle time on the websocket before disconnect.
     * @param maxMessageSize Maximum message size allowed on this websocket.
     * @param sendPingOnSubscribe testing option to ping the client when subscribe is ready.
     * @param verboseErrors whether or not to send verbose errors.
     */
    protected SubscriptionWebSocket(
            Elide elide,
            ExecutorService executorService,
            int connectTimeoutMs,
            int maxSubscriptions,
            UserFactory userFactory,
            long maxIdleTimeoutMs,
            int maxMessageSize,
            boolean sendPingOnSubscribe,
            boolean verboseErrors
    ) {
        this.elide = elide;
        this.executorService = executorService;
        this.connectTimeoutMs = connectTimeoutMs;
        this.maxSubscriptions = maxSubscriptions;
        this.userFactory = userFactory;
        this.sendPingOnSubscribe = sendPingOnSubscribe;
        this.maxIdleTimeoutMs = maxIdleTimeoutMs;
        this.maxMessageSize = maxMessageSize;
        this.verboseErrors = verboseErrors;

        EntityDictionary dictionary = elide.getElideSettings().getDictionary();
        for (String apiVersion : dictionary.getApiVersions()) {
            NonEntityDictionary nonEntityDictionary =
                    new NonEntityDictionary(DefaultClassScanner.getInstance(), CoerceUtil::lookup);

            SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary, nonEntityDictionary,
                    new SubscriptionDataFetcher(nonEntityDictionary), NO_VERSION);

            GraphQL api = GraphQL.newGraphQL(builder.build())
                    .queryExecutionStrategy(new AsyncSerialExecutionStrategy())
                    .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                    .build();

            apis.put(apiVersion, api);
        }
    }

    /**
     * Called on session open.
     * @param session The platform specific session object.
     * @throws IOException If there is an underlying error.
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {
        log.debug("Session Opening: {}", session.getId());
        SessionHandler subscriptionSession = createSessionHandler(session);

        session.setMaxIdleTimeout(maxIdleTimeoutMs);
        session.setMaxTextMessageBufferSize(maxMessageSize);
        session.setMaxBinaryMessageBufferSize(maxMessageSize);

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
        log.debug("Session Message: {} {}", session.getId(), message);

        SessionHandler handler = findSession(session);
        if (handler != null) {
            handler.handleRequest(message);
        } else {
            throw new IllegalStateException("Cannot locate session: " + session.getId());
        }
    }

    /**
     * Called on session close.
     * @param session The platform specific session object.
     * @throws IOException If there is an underlying error.
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        log.debug("Session Closing: {}", session.getId());
        SessionHandler handler = findSession(session);

        if (handler != null) {
            handler.safeClose(WebSocketCloseReasons.NORMAL_CLOSE);
            openSessions.remove(session);
        }
    }

    /**
     * Called on a session error.
     * @param session The platform specific session object.
     * @param throwable The error that occurred.
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("Session Error: {} {}", session.getId(), throwable.getMessage());
        SessionHandler handler = findSession(session);

        if (handler != null) {
            handler.safeClose(WebSocketCloseReasons.INTERNAL_ERROR);
            openSessions.remove(session);
        }
    }

    private SessionHandler findSession(Session wrappedSession) {
        SessionHandler sessionHandler = openSessions.getOrDefault(wrappedSession, null);

        String message = "Unable to locate active session: " + wrappedSession.getId();
        if (sessionHandler == null) {
            log.error(message);
        }
        return sessionHandler;
    }

    protected SessionHandler createSessionHandler(Session session) {
        String apiVersion = session.getRequestParameterMap().getOrDefault("ApiVersion",
                List.of(NO_VERSION)).get(0);

        User user = userFactory.create(session);

        return new SessionHandler(session, elide.getDataStore(), elide, apis.get(apiVersion),
                connectTimeoutMs, maxSubscriptions,
                ConnectionInfo.builder()
                        .user(user)
                        .baseUrl(session.getRequestURI().getPath())
                        .parameters(session.getRequestParameterMap())
                        .getApiVersion(apiVersion).build(),
                sendPingOnSubscribe,
                verboseErrors,
                executorService);
    }
}
