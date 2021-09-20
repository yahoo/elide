/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

/**
 * All the message names of the graphql-ws protocol.
 */
public enum MessageType {
    CONNECTION_INIT("connection_init"),
    CONNECTION_ACK("connection_ack"),
    PING("ping"),
    PONG("pong"),
    SUBSCRIBE("subscribe"),
    NEXT("next"),
    ERROR("error"),
    COMPLETE("complete");

    String name;

    MessageType(String name) {
        this.name = name;
    }
}
