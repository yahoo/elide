/*
 * Copyright 2015, Yahoo Inc.
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
        super(message);
    }
    public InternalServerErrorException(Exception e) {
        super(e.toString(), e);
    }

    @Override
    public int getStatus() {
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
}
