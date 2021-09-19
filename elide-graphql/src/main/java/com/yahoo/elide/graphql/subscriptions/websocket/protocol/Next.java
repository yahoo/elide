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
    ExecutionResult result;

    @Builder
    @JsonCreator
    public Next(
            @JsonProperty("id") String id,
            @JsonProperty("result") ExecutionResult result
    ) {
        super(id, MessageType.NEXT);
        this.result = result;
    }
}
