/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import lombok.Getter;

/**
 * Elide response object.
 */
public class ElideResponse {
    @Getter private final int responseCode;
    @Getter private final String body;

    /**
     * Constructor.
     *
     * @param responseCode HTTP response code
     * @param body returned body string
     */
    public ElideResponse(int responseCode, String body) {
        this.responseCode = responseCode;
        this.body = body;
    }
}
