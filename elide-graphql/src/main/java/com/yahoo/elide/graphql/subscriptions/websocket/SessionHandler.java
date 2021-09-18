/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.graphql.subscriptions.ConnectionInfo;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    AtomicLong lastActiveEpoch;
    long inactivityTimeoutMs = 10000;
    boolean intialized = false;

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
            ConnectionInfo connectionInfo) {
        this.wrappedSession = wrappedSession;
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.connectionInfo = connectionInfo;
        this.mapper = elide.getMapper().getObjectMapper();
        this.activeRequests = new ConcurrentHashMap<>();
        this.lastActiveEpoch.set(System.currentTimeMillis());
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

    public synchronized void closeIfInactive() throws IOException {
        long currentEpoch = System.currentTimeMillis();
        if (currentEpoch - inactivityTimeoutMs > lastActiveEpoch.get()) {
            log.debug("Inactivity timeout");
            close();
        }
    }

    /**
     * Handles an incoming graphql-ws protocol message.
     * @param message The protocol message.
     */
    public void handleRequest(String message) {
        try {
            JsonNode type = mapper.readTree(message).get("type");

            if (type == null) {
                log.error("No type field. Web socket not following graphql-js protocol");
                return;
            }

            MessageType messageType;
            try {
                messageType = MessageType.valueOf(type.textValue());
            } catch (IllegalArgumentException e) {
                log.error("Unknown protocol message type {}", type.textValue());
                return;
            }

            switch (messageType) {
                case PING: {
                    if (intialized) {
                        lastActiveEpoch.set(System.currentTimeMillis());
                    }
                    safeSendPong();
                    break;
                }
                case CONNECTION_INIT: {
                    intialized = true;
                    lastActiveEpoch.set(System.currentTimeMillis());
                    safeSendConnectionAck();
                    break;
                }
                case SUBSCRIBE: {

                    Subscribe subscribe = mapper.readValue(message, Subscribe.class);
                    String protocolID = subscribe.getId();

                    if (activeRequests.containsKey(protocolID)) {
                        log.error("Active request already exists with ID: {}", protocolID);
                        break;
                    }

                    intialized = true;
                    lastActiveEpoch.set(System.currentTimeMillis());

                    RequestHandler requestHandler = new RequestHandler(this,
                            topicStore, elide, api, protocolID, UUID.randomUUID(), connectionInfo);

                    if (requestHandler.handleRequest(subscribe)) {
                        activeRequests.put(protocolID, requestHandler);
                    }
                    break;
                } default: {
                    log.error("Invalid protocol message type {}", type.textValue());
                }
            }
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    protected void safeSendConnectionAck() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        ConnectionAck ack = new ConnectionAck();

        try {
            sendMessage(mapper.writeValueAsString(ack));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeSendPong() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Pong pong = new Pong();

        try {
            sendMessage(mapper.writeValueAsString(pong));
        } catch (IOException e) {
            log.error(e.getMessage());
            safeClose();
        }
    }

    protected void safeClose() {
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
}
