/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;

/**
 * Access to the requested resource is.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_FORBIDDEN forbidden}
 */
public class ForbiddenAccessException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public ForbiddenAccessException() {
        this(null);
    }

    public ForbiddenAccessException(String message) {
        super(message);
    }

    @Override
    public int getStatus() {
        return HttpStatus.SC_FORBIDDEN;
    }
}
