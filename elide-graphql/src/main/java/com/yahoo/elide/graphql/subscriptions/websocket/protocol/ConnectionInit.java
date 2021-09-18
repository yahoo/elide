/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

public class ConnectionInit extends AbstractProtocolMessage {
    public ConnectionInit() {
        super(MessageType.CONNECTION_INIT);
    }
}
