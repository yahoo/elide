/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;

/**
 * Unknown entity exception.
 */
public class UnknownEntityException extends HttpStatusException {
    public UnknownEntityException(String entityType) {
        super(HttpStatus.SC_BAD_REQUEST, "Unknown entity type: '" + entityType + "'");
    }
}
