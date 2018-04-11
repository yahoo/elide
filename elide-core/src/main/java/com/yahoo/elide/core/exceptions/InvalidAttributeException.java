/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;
import com.yahoo.elide.core.HttpStatus;

/**
 * Requested object ID is.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_NOT_FOUND invalid}
 */
public class InvalidAttributeException extends HttpStatusException {
    public InvalidAttributeException(String attributeName, String type, Throwable cause) {
        super(HttpStatus.SC_NOT_FOUND, "Unknown attribute '" + attributeName + "' in '" + type + "'", cause, null);
    }

    public InvalidAttributeException(String attributeName, String type) {
        this(attributeName, type, null);
    }

    public InvalidAttributeException(String message, Throwable cause) {
        super(HttpStatus.SC_NOT_FOUND, message, cause, null);
    }
}
