/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.subscriptions.websocket.protocol;

import jakarta.websocket.CloseReason;
import lombok.Getter;

/**
 * Reasons the server will disconnect the web socket.
 * See (graphql-ws) protocol.
 */
public class WebSocketCloseReasons {
    public enum CloseCode {
        CONNECTION_TIMEOUT(4408),
        MULTIPLE_INIT(4429),
        UNAUTHORIZED(4401),
        INVALID_MESSAGE(4400),
        MAX_SUBSCRIPTIONS(4300),  //This is our own message (not part of the protocol)
        DUPLICATE_ID(4409);

        @Getter
        private int code;

        CloseCode(int code) {
            this.code = code;
        }

        public CloseReason toReason(String reason) {
            return new CloseReason(createCloseCode(code), reason);
        }

    }

    public static final CloseReason CONNECTION_TIMEOUT =
            CloseCode.CONNECTION_TIMEOUT.toReason("Connection initialisation timeout");

    public static final CloseReason MULTIPLE_INIT =
            CloseCode.MULTIPLE_INIT.toReason("Too many initialisation requests");

    public static final CloseReason UNAUTHORIZED =
            CloseCode.UNAUTHORIZED.toReason("Unauthorized");

    public static final CloseReason INVALID_MESSAGE =
            CloseCode.INVALID_MESSAGE.toReason("Invalid message");

    public static final CloseReason INTERNAL_ERROR =
            new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Internal Error");

    public static final CloseReason NORMAL_CLOSE =
            new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal Closure");

    public static final CloseReason MAX_SUBSCRIPTIONS =
            CloseCode.MAX_SUBSCRIPTIONS.toReason("Exceeded max subscriptions");

    public static CloseReason.CloseCode createCloseCode(final int code) {
        return new CloseReason.CloseCode() {
            @Override
            public int getCode() {
                return code;
            }
        };
    }
}
