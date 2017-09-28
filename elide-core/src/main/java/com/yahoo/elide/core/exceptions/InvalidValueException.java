/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;
import com.yahoo.elide.core.HttpStatus;

/**
 * Exception when an invalid value is used.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_BAD_REQUEST invalid}
 */
public class InvalidValueException extends HttpStatusException {

    public InvalidValueException(Object value) {
        this(value, null);
    }

    public InvalidValueException(Object value, String verboseMessage) {
        super(HttpStatus.SC_BAD_REQUEST, "Invalid value: " + value, null, () -> verboseMessage);
    }

    public InvalidValueException(String message, Throwable cause) {
        super(HttpStatus.SC_BAD_REQUEST, message, cause, null);
    }
}
