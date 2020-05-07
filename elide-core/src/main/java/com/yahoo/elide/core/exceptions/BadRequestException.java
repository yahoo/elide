/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;

/**
 * Invalid predicate exception.
 */
public class BadRequestException extends HttpStatusException {
    public BadRequestException(String message) {
        super(HttpStatus.SC_BAD_REQUEST, message);
    }
}
