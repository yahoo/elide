/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import lombok.Builder;

public class Complete extends AbstractProtocolMessageWithID {

    @Builder
    public Complete(String id) {
        super(id, MessageType.COMPLETE);
    }
}
