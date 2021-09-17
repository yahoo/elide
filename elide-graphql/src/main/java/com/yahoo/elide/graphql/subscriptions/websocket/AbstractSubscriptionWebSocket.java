/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import com.yahoo.elide.Elide;
import com.yahoo.elide.graphql.ExecutionResultSerializer;
import com.yahoo.elide.graphql.GraphQLErrorSerializer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class AbstractSubscriptionWebSocket<T extends Closeable> {
    ConcurrentMap<T, AbstractSession> openSessions = new ConcurrentHashMap<>();

    public AbstractSubscriptionWebSocket(Elide elide) {
        GraphQLErrorSerializer errorSerializer = new GraphQLErrorSerializer();
        SimpleModule module = new SimpleModule("ExecutionResultSerializer", Version.unknownVersion());
        module.addSerializer(ExecutionResult.class, new ExecutionResultSerializer(errorSerializer));
        module.addSerializer(GraphQLError.class, errorSerializer);
        elide.getElideSettings().getMapper().getObjectMapper().registerModule(module);
    }

    public void onOpen(T session) throws IOException {
        AbstractSession<T> subscriptionSession = createSession(session);

        openSessions.put(session, subscriptionSession);
    }

    public void onMessage(T session, String message) throws IOException {
        findSession(session).handleRequest(message);
    }

    public void onClose(T session) throws IOException {
        findSession(session).safeClose();
        openSessions.remove(session);
    }

    public void onError(T session, Throwable throwable) {
        log.error(throwable.getMessage());
        findSession(session).safeClose();
        openSessions.remove(session);
    }

    private AbstractSession<T> findSession(T wrappedSession) {
        AbstractSession<T> subscriptionSession = openSessions.getOrDefault(wrappedSession, null);

        String message = "Unable to locate active session associated with: " + wrappedSession.toString();
        log.error(message);
        if (subscriptionSession == null) {
            throw new IllegalStateException(message);
        }
        return subscriptionSession;
    }

    protected abstract AbstractSession<T> createSession(T wrappedSession);
}
