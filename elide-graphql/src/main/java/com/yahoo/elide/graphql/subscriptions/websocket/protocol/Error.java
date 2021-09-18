/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

public class Error extends AbstractProtocolMessageWithID {

    public Error(String id) {
        super(id, MessageType.ERROR);
    }
}
