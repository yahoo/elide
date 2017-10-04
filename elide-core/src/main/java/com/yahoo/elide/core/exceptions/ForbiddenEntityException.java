/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;

/**
 * Requested object is.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_NOT_FOUND invalid}
 */
public class ForbiddenEntityException extends HttpStatusException {
    public ForbiddenEntityException() {
        super(HttpStatus.SC_NOT_FOUND, String.format("Entity not found"));
    }
}
