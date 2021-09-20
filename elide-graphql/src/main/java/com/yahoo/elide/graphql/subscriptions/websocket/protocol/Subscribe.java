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
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * Create a subscription (client -> server)
 */
@Value
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "id", "operationName", "query", "variables"})
public class Subscribe extends AbstractProtocolMessageWithID {
    String operationName;

    @JsonProperty(required = true)
    String query;

    Map<String, Object> variables;

    @Builder
    @JsonCreator
    public Subscribe(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty("operationName") String operationName,
            @JsonProperty(value = "query", required = true) String query,
            @JsonProperty("variables") Map<String, Object> variables) {
        super(id, MessageType.SUBSCRIBE);
        this.operationName = operationName;
        this.query = query;
        if (variables == null) {
            this.variables = new HashMap<>();
        } else {
            this.variables = variables;
        }
    }
}
