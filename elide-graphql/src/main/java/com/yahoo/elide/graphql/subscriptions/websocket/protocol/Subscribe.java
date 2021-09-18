/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
@Value
public class Subscribe extends AbstractProtocolMessageWithID {
    String operationName;
    String query;
    Map<String, Object> variables;

    @Builder
    public Subscribe(String id, String operationName, String query, Map<String, Object> variables) {
        super(id, MessageType.SUBSCRIBE);
        this.operationName = operationName;
        this.query = query;
        this.variables = variables;
    }
}
