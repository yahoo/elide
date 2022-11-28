/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Base class of all graphql-ws protocol messages.
 */
@AllArgsConstructor
@Getter
public abstract class AbstractProtocolMessage {

    @JsonProperty(required = true)
    final MessageType type;
}
