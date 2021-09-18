/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import graphql.ExecutionResult;
import lombok.Builder;
import lombok.Value;

@Value
public class Next extends AbstractProtocolMessageWithID {
    ExecutionResult result;

    @Builder
    public Next(String id, ExecutionResult result) {
        super(id, MessageType.NEXT);
        this.result = result;
    }
}
