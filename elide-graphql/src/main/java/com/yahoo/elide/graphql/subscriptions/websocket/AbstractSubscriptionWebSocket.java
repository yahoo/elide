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

/**
 * Given that web socket APIs are all different across platforms, this class provides an abstraction
 * with all the common logic needed to pull subscription messages from Elide.
 * @param <T> The platform specific session type.
 */
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

    /**
     * Called on session open.
     * @param session The platform specific session object.
     * @throws IOException If there is an underlying error.
     */
    public void onOpen(T session) throws IOException {
        AbstractSession<T> subscriptionSession = createSession(session);

        openSessions.put(session, subscriptionSession);
    }

    /**
     * Called on a new web socket message.
     * @param session The platform specific session object.
     * @param message THe new message.
     * @throws IOException If there is an underlying error.
     */
    public void onMessage(T session, String message) throws IOException {
        findSession(session).handleRequest(message);
    }

    /**
     * Called on session close.
     * @param session The platform specific session object.
     * @throws IOException If there is an underlying error.
     */
    public void onClose(T session) throws IOException {
        findSession(session).safeClose();
        openSessions.remove(session);
    }

    /**
     * Called on a session error.
     * @param session The platform specific session object.
     * @param throwable The error that occurred.
     */
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
