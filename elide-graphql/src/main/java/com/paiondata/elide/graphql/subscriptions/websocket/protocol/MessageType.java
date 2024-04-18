/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonValue;

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

    private String name;

    @JsonValue
    public String getName() {
        return name;
    }

    MessageType(String name) {
        this.name = name;
    }
}
