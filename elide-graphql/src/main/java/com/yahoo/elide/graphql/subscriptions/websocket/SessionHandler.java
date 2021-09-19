/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.ConnectionAck;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.MessageType;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Pong;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Subscribe;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Given that web socket APIs are all different across platforms, this class provides an abstraction
 * with all the common logic needed to pull subscription messages from Elide.
 * @param <T> The platform specific session type.
 */
@Slf4j
public abstract class SessionHandler<T extends Closeable> implements Closeable {
    protected DataStore topicStore;
    protected Elide elide;
    protected GraphQL api;
    protected T wrappedSession;
    Map<String, RequestHandler> activeRequests;
    ConnectionInfo connectionInfo;
    ObjectMapper mapper;
    long connectionTimeoutMs;
    Thread timeoutThread;
    boolean initialized = false;

    /**
     * Constructor.
     * @param wrappedSession The underlying platform session object.
     * @param topicStore The JMS data store.
     * @param elide Elide instance.
     * @param api GraphQL api.
     * @param connectionInfo Connection metadata.
     */
    public SessionHandler(
            T wrappedSession,
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            long connectionTimeoutMs,
            ConnectionInfo connectionInfo) {
        this.wrappedSession = wrappedSession;
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.connectionInfo = connectionInfo;
        this.mapper = elide.getMapper().getObjectMapper();
        this.activeRequests = new HashMap<>();
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.timeoutThread = new Thread(new ConnectionTimer());
        timeoutThread.start();
    }

    /**
     * Close this session.
     * @throws IOException
     */
    public synchronized void close() throws IOException {
        activeRequests.forEach((protocolID, handler) -> {
            handler.safeClose();
        });
        wrappedSession.close();
    }

    public synchronized void close(String protocolID) {
        activeRequests.remove(protocolID);
    }

    /**
     * Handles an incoming graphql-ws protocol message.
     * @param message The protocol message.
     */
    public void handleRequest(String message) {
        try {
            JsonNode type = mapper.readTree(message).get("type");

            if (type == null) {
                safeClose(4400, "Missing type field");
                return;
            }

            MessageType messageType;
            try {
                messageType = MessageType.valueOf(type.textValue());
            } catch (IllegalArgumentException e) {
                safeClose(4400, "Unknown protocol message type " + type.textValue());
                return;
            }

            switch (messageType) {
                case PING: {
                    safeSendPong();
                    break;
                }
                case CONNECTION_INIT: {
                    if (initialized) {
                        safeClose(4429, "Too many initialization requests");
                        return;
                    }

                    safeSendConnectionAck();
                    initialized = true;
                    break;
                }
                case SUBSCRIBE: {

                    if (!initialized) {
                        safeClose(4401, "Unauthorized");
                        return;
                    }

                    Subscribe subscribe = mapper.readValue(message, Subscribe.class);
                    String protocolID = subscribe.getId();

                    synchronized (this) {
                        if (activeRequests.containsKey(protocolID)) {
                            safeClose(4409, "Subscriber for " + protocolID + " already exists");
                            return;
                        }

                        timeoutThread.interrupt();

                        RequestHandler requestHandler = new RequestHandler(this,
                                topicStore, elide, api, protocolID, UUID.randomUUID(), connectionInfo);

                        if (requestHandler.handleRequest(subscribe)) {
                            activeRequests.put(protocolID, requestHandler);
                        }
                    }
                    break;
                } default: {
                    safeClose(4400, "Invalid protocol message type " + type.textValue());
                }
            }
        } catch (JsonProcessingException e) {
            safeClose(4400, "Invalid message body");
        }
    }

    protected void safeSendConnectionAck() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        ConnectionAck ack = new ConnectionAck();

        try {
            sendMessage(mapper.writeValueAsString(ack));
        } catch (IOException e) {
            safeClose(5000, e.getMessage());
        }
    }

    protected void safeSendPong() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Pong pong = new Pong();

        try {
            sendMessage(mapper.writeValueAsString(pong));
        } catch (IOException e) {
            safeClose(5000, e.getMessage());
        }
    }

    protected void safeClose(Integer code, String message) {
        String errorMessage = code.toString() + ": " + message;
        log.debug(errorMessage);
        try {
            sendMessage(errorMessage);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        try {
            close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Send a text message on the native session.
     * @param message The message to send.
     * @throws IOException
     */
    public abstract void sendMessage(String message) throws IOException;

    private class ConnectionTimer implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(connectionTimeoutMs);
                synchronized (SessionHandler.this) {
                    if (activeRequests.size() == 0) {
                        safeClose(4408, "Connection initialisation timeout");
                    }
                }
            } catch (InterruptedException e) {
                log.debug("Timeout thread interrupted: " + e.getMessage());
            }
        }
    }
}
