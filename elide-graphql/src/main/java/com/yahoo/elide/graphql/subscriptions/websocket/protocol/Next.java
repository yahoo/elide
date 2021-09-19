/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import graphql.ExecutionResult;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "type", "id", "result"})
public class Next extends AbstractProtocolMessageWithID {
    @JsonProperty(required = true)
    ExecutionResult payload;

    @Builder
    @JsonCreator
    public Next(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "result", required = true) ExecutionResult result
    ) {
        super(id, MessageType.NEXT);
        this.payload = result;
    }
}
