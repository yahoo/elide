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
     * @param user The elide user.
     */
    public SubscriptionSession(DataStore topicStore,
                               Elide elide,
                               GraphQL api,
                               Session wrappedSession,
                               int connectTimeoutMs,
                               int maxSubscriptions,
                               User user) {
        super(wrappedSession, topicStore, elide, api, connectTimeoutMs, maxSubscriptions,
                ConnectionInfo.builder()
                        .user(user)
                        .baseUrl(wrappedSession.getRequestURI().getPath())
                        .parameters(wrappedSession.getRequestParameterMap())
                        .build());
    }

    @Override
    public void sendMessage(String message) throws IOException {
        wrappedSession.getBasicRemote().sendText(message);
    }
}
