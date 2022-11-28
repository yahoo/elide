/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

/**
 * Base class of all graphql-ws messages that take an ID field.
 */
@Getter
public abstract class AbstractProtocolMessageWithID extends AbstractProtocolMessage {

    @JsonProperty(required = true)
    final String id;

    public AbstractProtocolMessageWithID(String id, MessageType type) {
        super(type);
        this.id = id;
    }
}
