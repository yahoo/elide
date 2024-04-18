/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.websocket.protocol;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * Start an incoming connection (client to server).
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ConnectionInit extends AbstractProtocolMessage {
    //Will contain authentication credentials.
    Map<String, Object> payload = new HashMap<>();

    public ConnectionInit() {
        super(MessageType.CONNECTION_INIT);
    }
}
