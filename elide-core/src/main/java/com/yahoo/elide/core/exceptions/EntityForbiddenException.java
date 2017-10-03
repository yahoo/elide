/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;

/**
 * Requested object ID is.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_NOT_FOUND invalid}
 */
public class EntityForbiddenException extends HttpStatusException {
    public EntityForbiddenException() {
        super(HttpStatus.SC_NOT_FOUND, String.format("Entity not found"));
    }
}
