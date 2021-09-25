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
import com.google.common.base.Preconditions;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Implements the graphql-ws protocol (https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)
 * over Elide subscriptions.
 */
@Slf4j
public class SessionHandler {
    protected DataStore topicStore;
    protected Elide elide;
    protected GraphQL api;
    protected Session wrappedSession;
    protected Map<String, RequestHandler> activeRequests;
    protected ConnectionInfo connectionInfo;
    protected ObjectMapper mapper;
    protected int connectionTimeoutMs;
    protected int maxSubscriptions;
    protected Thread timeoutThread;
    protected boolean initialized = false;
    protected boolean sendPingOnSubscribe = false;
    protected ExecutorService executorService;

    /**
     * Constructor.
     * @param wrappedSession The underlying platform session object.
     * @param topicStore The JMS data store.
     * @param elide Elide instance.
     * @param api GraphQL api.
     * @param connectionTimeoutMs Connection timeout in milliseconds.
     * @param maxSubscriptions Max number of outstanding subscriptions per web socket.
     * @param connectionInfo Connection metadata.
     * @param sendPingOnSubscribe Sends a ping on subscribe message (to aid with testing).
     * @param executorService Executor Service to launch threads.
     */
    public SessionHandler(
            Session wrappedSession,
            DataStore topicStore,
            Elide elide,
            GraphQL api,
            int connectionTimeoutMs,
            int maxSubscriptions,
            ConnectionInfo connectionInfo,
            boolean sendPingOnSubscribe,
            ExecutorService executorService) {
        Preconditions.checkState(maxSubscriptions > 0);
        this.wrappedSession = wrappedSession;
        this.topicStore = topicStore;
        this.elide = elide;
        this.api = api;
        this.connectionInfo = connectionInfo;
        this.mapper = elide.getMapper().getObjectMapper();
        this.activeRequests = new HashMap<>();
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.maxSubscriptions = maxSubscriptions;
        this.sendPingOnSubscribe = sendPingOnSubscribe;
        if (executorService == null) {
            this.executorService = Executors.newFixedThreadPool(maxSubscriptions);
        } else {
            this.executorService = executorService;
        }
        this.timeoutThread = new Thread(new ConnectionTimer());
        this.timeoutThread.start();
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
        executorService.shutdown();
    }

    protected synchronized void close(String protocolID) {
        activeRequests.remove(protocolID);
    }

    /**
     * Handles an incoming graphql-ws protocol message.
     * @param message The protocol message.
     */
    public void handleRequest(String message) {
        log.debug("Received Message: {} {}", wrappedSession.getId(), message);
        try {
            JsonNode type = mapper.readTree(message).get("type");

            if (type == null) {
                safeClose(INVALID_MESSAGE);
                return;
            }

            MessageType messageType;
            try {
                messageType = MessageType.valueOf(type.textValue().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                safeClose(INVALID_MESSAGE);
                return;
            }

            switch (messageType) {
                case PING: {
                    handlePing();
                    return;
                }
                case PONG: {
                    //Ignore
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

        timeoutThread.interrupt();

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

        RequestHandler requestHandler = new RequestHandler(this,
                topicStore, elide, api, protocolID, UUID.randomUUID(), connectionInfo, sendPingOnSubscribe);

        activeRequests.put(protocolID, requestHandler);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                requestHandler.handleRequest(subscribe);
            }
        });
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
        log.debug("Closing session handler: {} {}", wrappedSession.getId(), reason);
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
    public synchronized void sendMessage(String message) throws IOException {
        wrappedSession.getBasicRemote().sendText(message);
    }

    /**
     * Closes the socket if SUBSCRIBE has not been received in the allotted time.
     */
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
