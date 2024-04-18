/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

/**
 * Requested object ID is.
 *
 * {@link HttpStatus#SC_BAD_REQUEST invalid}
 */
public class InvalidEntityBodyException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public InvalidEntityBodyException(String body) {
        this(body, null);
    }

    public InvalidEntityBodyException(String body, Throwable cause) {
        super(HttpStatus.SC_BAD_REQUEST, "Bad Request Body'" + body + "'", cause, null);
    }
}
