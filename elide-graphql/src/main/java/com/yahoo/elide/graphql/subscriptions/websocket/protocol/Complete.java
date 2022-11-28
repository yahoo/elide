/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Builder;

/**
 * Send on subscription completion (bidirectional).
 */
@JsonPropertyOrder({"type", "id"})
public class Complete extends AbstractProtocolMessageWithID {

    @Builder
    @JsonCreator
    public Complete(
            @JsonProperty(value = "id", required = true) String id
    ) {
        super(id, MessageType.COMPLETE);
    }
}
