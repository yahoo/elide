/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

/**
 * Requested object ID is.
 *
 * {@link HttpStatus#SC_LOCKED}
 */
public class TransactionException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public TransactionException(Throwable e) {
        super(HttpStatus.SC_LOCKED, formatExceptionCause(e), e, null);
    }
}
