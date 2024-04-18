/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.websocket;

import com.paiondata.elide.Elide;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.BasicApiVersionValidator;
import com.paiondata.elide.core.request.route.FlexibleRouteResolver;
import com.paiondata.elide.core.request.route.NullRouteResolver;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.graphql.NonEntityDictionary;
import com.paiondata.elide.graphql.subscriptions.SubscriptionDataFetcher;
import com.paiondata.elide.graphql.subscriptions.SubscriptionModelBuilder;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons;

import graphql.GraphQL;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.schema.validation.InvalidSchemaException;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * JSR-356 Implementation of a programmatic web socket endpoint for GraphQL
 * subscriptions. JSR-356 should allow cross-platform use for Spring, Quarkus,
 * and other containers.
 * <p>
 * This requires programmatic registration using a
 * {@link jakarta.websocket.server.ServerEndpointConfig.Builder} with the
 * graphql-transport-ws subprotocol as the
 * {@link jakarta.websocket.server.ServerEndpoint} annotation requires that the
 * annotated class have a public no-arg constructor.
 */
@Slf4j
@Builder
public class SubscriptionWebSocket extends Endpoint {
    public static final String MEDIA_TYPE = "application/json";
    public static final String SUBPROTOCOL_GRAPHQL_TRANSPORT_WS = "graphql-transport-ws";
    public static final List<String> SUPPORTED_WEBSOCKET_SUBPROTOCOLS = List.of(SUBPROTOCOL_GRAPHQL_TRANSPORT_WS);

    private Elide elide;
    private ExecutorService executorService;

    @Builder.Default
    private Duration connectionTimeout = Duration.ofMillis(5000);

    @Builder.Default
    private int maxSubscriptions = 30;

    @Builder.Default
    private UserFactory userFactory = DEFAULT_USER_FACTORY;

    @Builder.Default
    private Duration maxIdleTimeout = Duration.ofMillis(300000);

    @Builder.Default
    private int maxMessageSize = 10000;

    @Builder.Default
    private boolean sendPingOnSubscribe = false;

    @Builder.Default
    private DataFetcherExceptionHandler dataFetcherExceptionHandler = new SimpleDataFetcherExceptionHandler();

    @Builder.Default
    private RouteResolver routeResolver = null;
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
     * @param connectionTimeout Connection timeout.
     * @param maxSubscriptions The maximum number of concurrent subscriptions per socket.
     * @param userFactory A function which creates an Elide user given a session object.
     * @param maxIdleTimeout Max idle time on the websocket before disconnect.
     * @param maxMessageSize Maximum message size allowed on this websocket.
     * @param sendPingOnSubscribe testing option to ping the client when subscribe is ready.
     */
    protected SubscriptionWebSocket(
            Elide elide,
            ExecutorService executorService,
            Duration connectionTimeout,
            int maxSubscriptions,
            UserFactory userFactory,
            Duration maxIdleTimeout,
            int maxMessageSize,
            boolean sendPingOnSubscribe,
            DataFetcherExceptionHandler dataFetcherExceptionHandler,
            RouteResolver routeResolver
    ) {
        this.elide = elide;
        this.executorService = executorService;
        this.connectionTimeout = connectionTimeout;
        this.maxSubscriptions = maxSubscriptions;
        this.userFactory = userFactory;
        this.sendPingOnSubscribe = sendPingOnSubscribe;
        this.maxIdleTimeout = maxIdleTimeout;
        this.maxMessageSize = maxMessageSize;
        this.dataFetcherExceptionHandler = dataFetcherExceptionHandler;
        this.routeResolver = routeResolver;

        EntityDictionary dictionary = elide.getElideSettings().getEntityDictionary();
        for (String apiVersion : dictionary.getApiVersions()) {
            NonEntityDictionary nonEntityDictionary =
                    new NonEntityDictionary(new DefaultClassScanner(), CoerceUtil::lookup);

            SubscriptionModelBuilder builder = new SubscriptionModelBuilder(dictionary, nonEntityDictionary,
                    elide.getElideSettings(), new SubscriptionDataFetcher(nonEntityDictionary), apiVersion);

            try {
                GraphQL api = GraphQL.newGraphQL(builder.build())
                        .defaultDataFetcherExceptionHandler(this.dataFetcherExceptionHandler)
                        .queryExecutionStrategy(new AsyncSerialExecutionStrategy(this.dataFetcherExceptionHandler))
                        .subscriptionExecutionStrategy(
                                new SubscriptionExecutionStrategy(this.dataFetcherExceptionHandler))
                        .build();
                apis.put(apiVersion, api);
            } catch (InvalidSchemaException e) {
                // There may not be subscription fields for the api version
                apis.put(apiVersion, null);
            }
        }
        if (this.routeResolver == null) {
            Set<String> apiVersions = elide.getElideSettings().getEntityDictionary().getApiVersions();
            if (apiVersions.size() == 1 && apiVersions.contains(EntityDictionary.NO_VERSION)) {
                this.routeResolver = new NullRouteResolver();
            } else {
                this.routeResolver = new FlexibleRouteResolver(new BasicApiVersionValidator(), () -> "");
            }

        }
    }

    /**
     * Called on session open.
     * @param session The platform specific session object.
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        log.debug("Session Opening: {}", session.getId());
        SessionHandler subscriptionSession = createSessionHandler(session);

        session.setMaxIdleTimeout(this.maxIdleTimeout.toMillis());
        session.setMaxTextMessageBufferSize(maxMessageSize);
        session.setMaxBinaryMessageBufferSize(maxMessageSize);

        openSessions.put(session, subscriptionSession);

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                SubscriptionWebSocket.this.onMessage(session, message);
            }
        });
    }

    /**
     * Called on a new web socket message.
     * @param session The platform specific session object.
     * @param message The new message.
     */
    public void onMessage(Session session, String message) {
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
     */
    @Override
    public void onClose(Session session, CloseReason closeReason) {
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
    @Override
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

    @SuppressWarnings("unchecked")
    protected SessionHandler createSessionHandler(Session session) {
        User user = userFactory.create(session);

        String path = session.getPathParameters().get("path");
        if (path == null) {
           path = "";
        }

        String baseUrl = getBaseUrl(session);
        if (!path.isBlank() && baseUrl.endsWith(path)) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - path.length());
        }

        Map<String, List<String>> headers = new LinkedHashMap<>(session.getRequestParameterMap());
        // Get the properties from the handshake set in SubscriptionWebSocketConfigurator
        if (session.getUserProperties().get("headers") instanceof Map handshakeHeaders) {
            headers.putAll(handshakeHeaders);
        }

        Route route = routeResolver.resolve(MEDIA_TYPE, baseUrl, path, headers,
                session.getRequestParameterMap());

        String apiVersion = route.getApiVersion();
        return new SessionHandler(session, elide.getDataStore(), elide, apis.get(apiVersion),
                connectionTimeout, maxSubscriptions,
                ConnectionInfo.builder()
                        .user(user)
                        .route(route)
                        .build(),
                sendPingOnSubscribe,
                executorService);
    }

    protected String getBaseUrl(Session session) {
        String baseUrl = "";
        if (session.getUserProperties().get("requestURI") instanceof URI requestUri) {
            String scheme = requestUri.getScheme();
            scheme = "ws".equals(scheme) ? "http" : "https";
            try {
                baseUrl = new URI(scheme, requestUri.getAuthority(), requestUri.getPath(), null, null).toString();
            } catch (URISyntaxException e) {
                baseUrl = "";
            }
        }
        return baseUrl;
    }
}
