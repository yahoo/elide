/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;

/**
 * Invalid predicate exception.
 */
public class InvalidPredicateException extends HttpStatusException {
    public InvalidPredicateException(String filter) {
        super("Unknown filter '" + filter + "'");
    }

    public InvalidPredicateException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatus() {
        return HttpStatus.SC_BAD_REQUEST;
    }
}
