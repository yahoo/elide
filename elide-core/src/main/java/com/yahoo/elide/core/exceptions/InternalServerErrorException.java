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
 * {@link com.yahoo.elide.core.HttpStatus#SC_INTERNAL_SERVER_ERROR invalid}
 */
public class InternalServerErrorException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public InternalServerErrorException(String message) {
        super(HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
    }

    public InternalServerErrorException(Throwable e) {
        super(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString(), e, null);
    }

    public InternalServerErrorException(String message, Throwable e) {
        super(HttpStatus.SC_INTERNAL_SERVER_ERROR, message, e, null);
    }
}
