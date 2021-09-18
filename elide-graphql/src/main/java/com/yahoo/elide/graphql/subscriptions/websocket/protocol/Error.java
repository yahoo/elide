/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import graphql.GraphQLError;
import lombok.Builder;
import lombok.Value;

@Value
public class Error extends AbstractProtocolMessageWithID {

    GraphQLError[] payload;

    @Builder
    public Error(String id, GraphQLError[] payload) {
        super(id, MessageType.ERROR);
        this.payload = payload;
    }
}
