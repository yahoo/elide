/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.graphql.subscriptions.websocket.protocol.ConnectionInit;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.MessageType;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Next;
import com.yahoo.elide.graphql.subscriptions.websocket.protocol.Subscribe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 * Test client for GraphQL subscriptions.  This class makes it simpler to write integration tests.
 */
@ClientEndpoint
@Slf4j
public class SubscriptionWebSocketTestClient {

    private CountDownLatch latch;
    private ObjectMapper mapper;
    private List<ExecutionResult> results;
    private Session session;
    private String query;
    private int expectedNumberOfMessages;

    /**
     * Constructor.
     * @param expectedNumberOfMessages The number of expected messages before notifying the test driver.
     * @param query The subscription query to run.
     */
    public SubscriptionWebSocketTestClient(int expectedNumberOfMessages, String query) {
        latch = new CountDownLatch(1);
        mapper = new ObjectMapper();
        results = new ArrayList<>();
        this.query = query;
        this.expectedNumberOfMessages = expectedNumberOfMessages;
    }


    @OnOpen
    public void onOpen(Session session) throws Exception {
        this.session = session;
        log.info("WebSocket opened: " + session.getId());

        session.getBasicRemote().sendText(mapper.writeValueAsString(new ConnectionInit()));
        log.info("Sent INIT");
    }

    @OnMessage
    public void onMessage(String text) throws Exception {
        log.info("Message received: " + text);

        JsonNode type = mapper.readTree(text).get("type");
        MessageType messageType = MessageType.valueOf(type.textValue());

        switch (messageType) {
            case CONNECTION_ACK: {
                log.info("ACK received");

                Subscribe subscribe = Subscribe.builder()
                        .id("1")
                        .query(query)
                        .build();

                session.getBasicRemote().sendText(mapper.writeValueAsString(subscribe));

                log.info("Subscribe sent.");

                break;
            }
            case NEXT: {
                Next next = mapper.readValue(text, Next.class);
                results.add(next.getPayload());
                expectedNumberOfMessages--;
                if (expectedNumberOfMessages <= 0) {
                    latch.countDown();
                }
                break;
            }
            case ERROR: {
                Error error = mapper.readValue(text, Error.class);
                log.info("ERROR: {}", error.getMessage());
                latch.countDown();
                break;
            }
            default: {
                break;
            }
        }
    }

    @OnClose
    public void onClose(CloseReason reason) throws Exception {
        log.info("Session closed: " + reason.getCloseCode() + " " + reason.getReasonPhrase());
        latch.countDown();
    }

    @OnError
    public void onError(Throwable t) throws Exception {
        log.info("Session error: " + t.getMessage());
        latch.countDown();
    }

    /**
     * Wait for the subscription to deliver N messages and return them.
     * @param waitInSeconds The number of seconds to wait before giving up.
     * @return The messages received.
     * @throws InterruptedException
     */
    public List<ExecutionResult> waitOnClose(int waitInSeconds) throws InterruptedException {
        latch.await(waitInSeconds, TimeUnit.SECONDS);
        return results;
    }
}
