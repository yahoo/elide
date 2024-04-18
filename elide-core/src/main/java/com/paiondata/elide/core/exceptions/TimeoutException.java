/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

/**
 * Thrown for request timeouts.
 *
 * {@link HttpStatus#SC_TIMEOUT}
 */
public class TimeoutException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public TimeoutException(Throwable e) {
        super(HttpStatus.SC_TIMEOUT, "Request Timeout", e, null);
    }
}
