/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import lombok.Getter;

import java.util.Optional;

/**
 * Elide response object.
 */
public class ElideResponse {
    @Getter private final int responseCode;
    @Getter private final String body;
    @Getter private final Optional<Throwable> failureReason;

    /**
     * Constructor.
     *
     * @param responseCode HTTP response code
     * @param body returned body string
     */
    @Deprecated
    public ElideResponse(int responseCode, String body) {
        this(responseCode, body, null);
    }

    /**
     * Constructor.
     *
     * @param responseCode HTTP response code
     * @param body returned body string
     * @param failureReason the reason the request failed
     */
    public ElideResponse(int responseCode, String body, Throwable failureReason) {
        this.responseCode = responseCode;
        this.body = body;
        this.failureReason = Optional.ofNullable(failureReason);
    }
}
