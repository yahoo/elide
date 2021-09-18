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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.websocket.Session;

/**
 * Concrete AbstractSession implementation that wraps JSR 356 session.
 */
public class SubscriptionSession extends RequestHandler<Session> {

    /**
     * Constructor.
     * @param topicStore JMS Store
     * @param elide Elide Instance
     * @param api GraphQL API
     * @param wrappedSession underlying JSR 356 session.
     * @param requestId a unique request/session ID.
     */
    public SubscriptionSession(DataStore topicStore,
                               Elide elide,
                               GraphQL api,
                               Session wrappedSession,
                               UUID requestId) {
        super(wrappedSession, topicStore, elide, api, requestId);
    }

    @Override
    public User getUser() {
        //TODO - we should find a standard way to suck in role information.
        return new User(wrappedSession.getUserPrincipal());
    }

    @Override
    public void sendMessage(String message) throws IOException {
        wrappedSession.getBasicRemote().sendText(message);
    }

    @Override
    public String getBaseUrl() {
        return wrappedSession.getRequestURI().getPath();
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return wrappedSession.getRequestParameterMap();
    }
}
