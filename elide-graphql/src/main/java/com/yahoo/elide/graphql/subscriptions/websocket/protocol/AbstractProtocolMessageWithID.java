/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import lombok.Getter;

@Getter
public abstract class AbstractProtocolMessageWithID extends AbstractProtocolMessage {

    final String id;
    public AbstractProtocolMessageWithID(String id, MessageType type) {
        super(type);
        this.id = id;
    }
}
