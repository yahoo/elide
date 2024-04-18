/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.websocket.protocol;

/**
 * Acknowledge an incoming connection (server to client).
 */
public class ConnectionAck extends AbstractProtocolMessage {
    public ConnectionAck() {
        super(MessageType.CONNECTION_ACK);
    }
}
