/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.datastore.DataStore;

import graphql.GraphQL;

import java.io.IOException;
import java.util.UUID;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * JSR 356 Implementation of a web socket endpoint.  Default endpoint for Elide subscriptions.
 */
@ServerEndpoint(value = "/")
public class SubscriptionEndpoint extends AbstractSubscriptionWebSocket<Session> {
    private final DataStore topicStore;
    private final Elide elide;
    private final GraphQL api;
    private final int connectTimeoutMs;
    private final int maxSubscriptions;

    public SubscriptionEndpoint(DataStore topicStore, Elide elide, GraphQL api) {
        this(topicStore, elide, api, 10000, 30);
    }

    /**
     * Constructor.
     * @param topicStore The JMS data store
     * @param elide Elide instance
     * @param api GraphQL API
     * @param connectTimeoutMs Connection timeout
     */
    public SubscriptionEndpoint(DataStore topicStore, Elide elide, GraphQL api,
                                int connectTimeoutMs,
                                int maxSubscriptions) {
        super(elide);
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.connectTimeoutMs = connectTimeoutMs;
        this.maxSubscriptions = maxSubscriptions;
    }

    @OnOpen
    @Override
    public void onOpen(Session session) throws IOException {
        super.onOpen(session);
    }

    @OnMessage
    @Override
    public void onMessage(Session session, String message) throws IOException {
        super.onMessage(session, message);
    }

    @OnClose
    @Override
    public void onClose(Session session) throws IOException {
        super.onClose(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable throwable) {
        super.onError(session, throwable);
    }

    @Override
    protected SessionHandler<Session> createSession(Session wrappedSession) {
        UUID requestId = UUID.randomUUID();

        return new SubscriptionSession(topicStore, elide, api,
                wrappedSession, connectTimeoutMs, maxSubscriptions, requestId);
    }
}
