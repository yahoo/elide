/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

/**
 * Requested object ID is.
 *
 * {@link HttpStatus#SC_BAD_REQUEST invalid}
 */
public class InvalidEntityBodyException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public InvalidEntityBodyException(String body) {
        super(HttpStatus.SC_BAD_REQUEST, "Bad Request Body'" + body + "'");
    }
}
