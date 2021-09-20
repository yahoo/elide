/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket;

import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.CONNECTION_TIMEOUT;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.CloseCode.DUPLICATE_ID;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.INTERNAL_ERROR;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.INVALID_MESSAGE;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.MAX_SUBSCRIPTIONS;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.MULTIPLE_INIT;
import static com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons.UNAUTHORIZED;
import com.yahoo.elide.Elide;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Complete;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.ConnectionAck;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.MessageType;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Pong;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Subscribe;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.WebSocketCloseReasons;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Given that web socket APIs are all different across platforms, this class provides an abstraction
 * with all the common logic needed to pull subscription messages from Elide.
 */
@Slf4j
public class SessionHandler {
    protected DataStore topicStore;
    protected Elide elide;
    protected GraphQL api;
    protected Session wrappedSession;
    Map<String, RequestHandler> activeRequests;
    ConnectionInfo connectionInfo;
    ObjectMapper mapper;
    int connectionTimeoutMs;
    int maxSubscriptions;
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
            Session wrappedSession,
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            int connectionTimeoutMs,
            int maxSubscriptions,
            ConnectionInfo connectionInfo) {
        this.wrappedSession = wrappedSession;
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.connectionInfo = connectionInfo;
        this.mapper = elide.getMapper().getObjectMapper();
        this.activeRequests = new HashMap<>();
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.maxSubscriptions = maxSubscriptions;
        this.timeoutThread = new Thread(new ConnectionTimer());
        timeoutThread.start();
    }

    /**
     * Close this session.
     * @throws IOException
     */
    public synchronized void close(CloseReason reason) throws IOException {

        //Iterator here to avoid concurrent modification exceptions.
        Iterator<Map.Entry<String, RequestHandler>> iterator = activeRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, RequestHandler> item = iterator.next();
            RequestHandler handler = item.getValue();
            handler.safeClose();

        }
        wrappedSession.close(reason);
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
                safeClose(INVALID_MESSAGE);
                return;
            }

            MessageType messageType;
            try {
                messageType = MessageType.valueOf(type.textValue());
            } catch (IllegalArgumentException e) {
                safeClose(INVALID_MESSAGE);
                return;
            }

            switch (messageType) {
                case PING: {
                    handlePing();
                    return;
                }
                case CONNECTION_INIT: {
                    handleConnectionInit();
                    return;
                }
                case COMPLETE: {
                    Complete complete = mapper.readValue(message, Complete.class);
                    handleComplete(complete);
                    return;
                }
                case SUBSCRIBE: {
                    Subscribe subscribe = mapper.readValue(message, Subscribe.class);
                    handleSubscribe(subscribe);
                    return;
                } default: {
                    safeClose(INVALID_MESSAGE);
                    return;
                }
            }
        } catch (JsonProcessingException e) {
            safeClose(INVALID_MESSAGE);
        }
    }

    protected void handlePing() {
        safeSendPong();
    }

    protected void handleConnectionInit() {
        if (initialized) {
            safeClose(MULTIPLE_INIT);
            return;
        }

        safeSendConnectionAck();
        initialized = true;
    }

    protected synchronized void handleSubscribe(Subscribe subscribe) {
        if (!initialized) {
            safeClose(UNAUTHORIZED);
            return;
        }

        String protocolID = subscribe.getId();

        if (activeRequests.containsKey(protocolID)) {
            safeClose(new CloseReason(WebSocketCloseReasons.createCloseCode(DUPLICATE_ID.getCode()),
                    "Subscriber for " + protocolID + " already exists"));
            return;
        }

        if (activeRequests.size() >= maxSubscriptions) {
            safeClose(MAX_SUBSCRIPTIONS);
            return;
        }

        timeoutThread.interrupt();

        RequestHandler requestHandler = new RequestHandler(this,
                topicStore, elide, api, protocolID, UUID.randomUUID(), connectionInfo);

        if (requestHandler.handleRequest(subscribe)) {
            activeRequests.put(protocolID, requestHandler);
        }
    }

    protected synchronized void handleComplete(Complete complete) {
        String protocolID = complete.getId();
        if (activeRequests.containsKey(protocolID)) {
            RequestHandler handler = activeRequests.remove(protocolID);
            handler.safeClose();
        }

        //Ignore otherwise
    }

    protected void safeSendConnectionAck() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        ConnectionAck ack = new ConnectionAck();

        try {
            sendMessage(mapper.writeValueAsString(ack));
        } catch (IOException e) {
            safeClose(INTERNAL_ERROR);
        }
    }

    protected void safeSendPong() {
        ObjectMapper mapper = elide.getElideSettings().getMapper().getObjectMapper();
        Pong pong = new Pong();

        try {
            sendMessage(mapper.writeValueAsString(pong));
        } catch (IOException e) {
            safeClose(INTERNAL_ERROR);
        }
    }

    protected void safeClose(CloseReason reason) {
        try {
            close(reason);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Send a text message on the native session.
     * @param message The message to send.
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        wrappedSession.getBasicRemote().sendText(message);
    }

    private class ConnectionTimer implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(connectionTimeoutMs);
                synchronized (SessionHandler.this) {
                    if (activeRequests.size() == 0) {
                        safeClose(CONNECTION_TIMEOUT);
                    }
                }
            } catch (InterruptedException e) {
                log.debug("Timeout thread interrupted: " + e.getMessage());
            }
        }
    }
}
