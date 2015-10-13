/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;
import com.yahoo.elide.core.HttpStatus;

/**
 * Requested object ID is
 * {@link com.yahoo.elide.core.HttpStatus#SC_NOT_FOUND invalid}
 */
public class InvalidObjectIdentifierException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public InvalidObjectIdentifierException(String id) {
        super("Unknown identifier '" + id + "'");
    }

    @Override
    public int getStatus() {
        return HttpStatus.SC_NOT_FOUND;
    }
}
