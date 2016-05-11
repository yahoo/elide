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
public class InvalidObjectIdentifierException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public InvalidObjectIdentifierException(String id, String objectOrFieldName) {
        super(HttpStatus.SC_NOT_FOUND, "Unknown identifier '" + id + "' for " + objectOrFieldName);
    }
}
