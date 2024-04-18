/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

/**
 * Invalid predicate exception.
 */
public class BadRequestException extends HttpStatusException {
    public BadRequestException(String message) {
        super(HttpStatus.SC_BAD_REQUEST, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(HttpStatus.SC_BAD_REQUEST, message, cause, null);
    }
}
