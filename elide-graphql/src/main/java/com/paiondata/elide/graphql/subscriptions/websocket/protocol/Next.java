/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.websocket.protocol;

import com.paiondata.elide.graphql.serialization.ExecutionResultDeserializer;
import com.paiondata.elide.graphql.serialization.ExecutionResultSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import graphql.ExecutionResult;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Next subscription message (server to client).
 */
@Value
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "type", "id", "payload"})
public class Next extends AbstractProtocolMessageWithID {
    @JsonProperty(required = true)
    @JsonDeserialize(as = ExecutionResult.class, using = ExecutionResultDeserializer.class)
    @JsonSerialize(as = ExecutionResult.class, using = ExecutionResultSerializer.class)
    ExecutionResult payload;

    @Builder
    @JsonCreator
    public Next(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "payload", required = true) ExecutionResult result
    ) {
        super(id, MessageType.NEXT);

        this.payload = result;
    }
}
