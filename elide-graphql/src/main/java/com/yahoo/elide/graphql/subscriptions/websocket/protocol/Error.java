/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import graphql.GraphQLError;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "type", "id", "payload"})
public class Error extends AbstractProtocolMessageWithID {

    GraphQLError[] payload;

    @Builder
    @JsonCreator
    public Error(
            @JsonProperty("id") String id,
            @JsonProperty("payload") GraphQLError[] payload
    ) {
        super(id, MessageType.ERROR);
        this.payload = payload;
    }
}
