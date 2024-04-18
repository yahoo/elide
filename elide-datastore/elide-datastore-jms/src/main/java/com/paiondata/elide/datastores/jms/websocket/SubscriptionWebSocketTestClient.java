/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jms.websocket;

import com.paiondata.elide.graphql.serialization.GraphQLModule;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.Complete;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.ConnectionInit;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.Error;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.MessageType;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.Next;
import com.paiondata.elide.graphql.subscriptions.websocket.protocol.Subscribe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import graphql.ExecutionResult;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test client for GraphQL subscriptions.  This class makes it simpler to write integration tests.
 */
@ClientEndpoint
@Slf4j
public class SubscriptionWebSocketTestClient {

    private CountDownLatch sessionLatch;
    private CountDownLatch subscribeLatch;
    private ObjectMapper mapper;
    private List<ExecutionResult> results;
    private Session session;
    private List<String> queries;
    private int expectedNumberOfMessages;
    private int expectedNumberOfSubscribes;
    boolean isOpen = false;

    /**
     * Constructor.
     * @param expectedNumberOfMessages The number of expected messages before notifying the test driver.
     * @param queries The subscription queries to run.
     */
    public SubscriptionWebSocketTestClient(int expectedNumberOfMessages, List<String> queries) {
        sessionLatch = new CountDownLatch(1);
        subscribeLatch = new CountDownLatch(1);
        results = new ArrayList<>();
        this.queries = queries;
        this.expectedNumberOfMessages = expectedNumberOfMessages;
        this.expectedNumberOfSubscribes = queries.size();
        this.mapper = new ObjectMapper();
        mapper.registerModule(new GraphQLModule());
    }


    @OnOpen
    public void onOpen(Session session) throws Exception {
        this.session = session;
        log.debug("WebSocket opened: " + session.getId());

        isOpen = true;

        session.getBasicRemote().sendText(mapper.writeValueAsString(new ConnectionInit()));
    }

    @OnMessage
    public void onMessage(String text) throws Exception {

        JsonNode type = mapper.readTree(text).get("type");
        MessageType messageType = MessageType.valueOf(type.textValue().toUpperCase(Locale.ROOT));

        switch (messageType) {
            case CONNECTION_ACK: {
                Integer id = 1;
                for (String query : queries) {
                    Subscribe subscribe = Subscribe.builder()
                            .id(id.toString())
                            .payload(Subscribe.Payload.builder()
                                    .query(query)
                                    .build())
                            .build();

                    session.getBasicRemote().sendText(mapper.writeValueAsString(subscribe));
                    id++;
                }

                break;
            }
            case NEXT: {
                Next next = mapper.readValue(text, Next.class);
                results.add(next.getPayload());
                expectedNumberOfMessages--;
                if (expectedNumberOfMessages <= 0) {
                    sessionLatch.countDown();
                }
                break;
            }
            case PING: {
                expectedNumberOfSubscribes--;
                if (expectedNumberOfSubscribes <= 0) {
                    subscribeLatch.countDown();
                }
                break;
            }
            case ERROR: {
                Error error = mapper.readValue(text, Error.class);
                log.error("ERROR: {}", error.getPayload());
                sessionLatch.countDown();
                break;
            }
            default: {
                break;
            }
        }
    }

    @OnClose
    public void onClose(CloseReason reason) throws Exception {
        log.debug("Session closed: " + reason.getCloseCode() + " " + reason.getReasonPhrase());
        isOpen = false;
        sessionLatch.countDown();
    }

    @OnError
    public void onError(Throwable t) throws Exception {
        log.error("Session error: " + t.getMessage());
        isOpen = false;
        sessionLatch.countDown();
    }

    public void sendClose() throws Exception {
        if (isOpen) {
            Integer id = 1;
            for (String query : queries) {
                session.getBasicRemote().sendText(mapper.writeValueAsString(new Complete(id.toString())));
                id++;
            }
            isOpen = false;
        }
    }

    /**
     * Wait for the subscription to deliver N messages and return them.
     * @param waitInSeconds The number of seconds to wait before giving up.
     * @return The messages received.
     * @throws InterruptedException
     */
    public List<ExecutionResult> waitOnClose(int waitInSeconds) throws InterruptedException {
        sessionLatch.await(waitInSeconds, TimeUnit.SECONDS);
        return results;
    }

    /**
     * Wait for the subscription to be setup.
     * @param waitInSeconds The number of seconds to wait before giving up.
     * @throws InterruptedException
     */
    public void waitOnSubscribe(int waitInSeconds) throws InterruptedException {
        subscribeLatch.await(waitInSeconds, TimeUnit.SECONDS);
    }
}
