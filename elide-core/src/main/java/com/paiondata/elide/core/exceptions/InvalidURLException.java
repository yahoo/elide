/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

/**
 * Requested object ID is.
 *
 * {@link HttpStatus#SC_INTERNAL_SERVER_ERROR invalid}
 */
public class InvalidURLException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public InvalidURLException(Exception e) {
        super(HttpStatus.SC_NOT_FOUND, e.getMessage());
    }
}
