/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.endpoints;

import com.google.inject.spi.Message;

import java.io.IOException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

public class GraphQLSubscriptionWebSocket {
    @ServerEndpoint(value = "/subscription")
    public class ChatEndpoint {

        @OnOpen
        public void onOpen(Session session) throws IOException {
            // Get session and WebSocket connection
        }

        @OnMessage
        public void onMessage(Session session, Message message) throws IOException {
            // Handle new messages
        }

        @OnClose
        public void onClose(Session session) throws IOException {
            // WebSocket connection closes
        }

        @OnError
        public void onError(Session session, Throwable throwable) {
            // Do error handling here
        }
    }
}
