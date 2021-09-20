/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.websocket.protocol;

import javax.websocket.CloseReason;

public class WebSocketCloseReasons {
    public static final CloseReason CONNECTION_TIMEOUT =
            new CloseReason(createCloseCode(4408), "Connection initialisation timeout");

    public static final CloseReason MULTIPLE_INIT =
            new CloseReason(createCloseCode(4429), "Too many initialisation requests");

    public static final CloseReason UNAUTHORIZED =
            new CloseReason(createCloseCode(4401), "Unauthorized");

    public static final CloseReason INVALID_MESSAGE =
            new CloseReason(createCloseCode(4400), "Invalid message");

    public static final CloseReason INTERNAL_ERROR =
            new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal Error");

    public static final CloseReason NORMAL_CLOSE =
            new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal Closure");

    public static final CloseReason MAX_SUBSCRIPTIONS =
            new CloseReason(createCloseCode(4300), "Exceeded max subscriptions");

    public static CloseReason.CloseCode createCloseCode(final int code) {
        return new CloseReason.CloseCode() {
            @Override
            public int getCode() {
                return code;
            }
        };
    }
}
