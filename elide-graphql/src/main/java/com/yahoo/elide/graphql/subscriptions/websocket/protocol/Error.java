/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import com.yahoo.elide.graphql.GraphQLErrorDeserializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import graphql.GraphQLError;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Error occurred during setup of the subscription (server to client).
 */
@Value
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "type", "id", "payload"})
public class Error extends AbstractProtocolMessageWithID {

    @JsonProperty(required = true)
    @JsonDeserialize(contentAs = GraphQLError.class, contentUsing = GraphQLErrorDeserializer.class)
    GraphQLError[] payload;

    @Builder
    @JsonCreator
    public Error(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "payload", required = true) GraphQLError[] payload
    ) {
        super(id, MessageType.ERROR);
        this.payload = payload;
    }
}
