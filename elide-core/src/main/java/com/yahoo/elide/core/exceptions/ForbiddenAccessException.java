/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;

/**
 * Access to the requested resource is.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_FORBIDDEN forbidden}
 */
public class ForbiddenAccessException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    private RequestScope scope;

    public ForbiddenAccessException(String verboseMessage, RequestScope scope) {
        super(null, verboseMessage);
        this.scope = scope;
    }

    public String getReason() {
        return scope.getAuthFailureReason();
    }

    @Override
    public int getStatus() {
        return HttpStatus.SC_FORBIDDEN;
    }
}
