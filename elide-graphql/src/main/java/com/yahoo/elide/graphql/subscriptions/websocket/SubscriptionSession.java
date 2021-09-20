/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.security.User;

import graphql.GraphQL;

import java.io.IOException;
import java.util.UUID;
import javax.websocket.Session;

/**
 * Concrete AbstractSession implementation that wraps JSR 356 session.
 */
public class SubscriptionSession extends SessionHandler<Session> {

    /**
     * Constructor.
     * @param topicStore JMS Store
     * @param elide Elide Instance
     * @param api GraphQL API
     * @param wrappedSession underlying JSR 356 session.
     * @param connectTimeoutMs connection timeout.
     * @param maxSubscriptions max number of subscriptions.
     * @param requestId a unique request/session ID.
     */
    public SubscriptionSession(DataStore topicStore,
                               Elide elide,
                               GraphQL api,
                               Session wrappedSession,
                               int connectTimeoutMs,
                               int maxSubscriptions,
                               UUID requestId) {
        super(wrappedSession, topicStore, elide, api, connectTimeoutMs, maxSubscriptions,
                ConnectionInfo.builder()
                        .user(new User(wrappedSession.getUserPrincipal()))
                        .baseUrl(wrappedSession.getRequestURI().getPath())
                        .parameters(wrappedSession.getRequestParameterMap())
                        .build());
    }

    @Override
    public void sendMessage(String message) throws IOException {
        wrappedSession.getBasicRemote().sendText(message);
    }
}
