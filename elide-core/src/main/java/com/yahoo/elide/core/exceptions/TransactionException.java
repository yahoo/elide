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
 * {@link com.yahoo.elide.core.HttpStatus#SC_LOCKED}
 */
public class TransactionException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public TransactionException(Throwable e) {
        super(formatMessage(e.getCause() == null ? e : e.getCause()), e);
    }

    private static String formatMessage(Throwable e) {
        return e == null ? null : e.getMessage() == null ? e.toString() : e.getMessage();
    }

    @Override
    public int getStatus() {
        return HttpStatus.SC_LOCKED;
    }
}
