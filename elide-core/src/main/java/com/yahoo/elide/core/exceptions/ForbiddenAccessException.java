/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Access to the requested resource is.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_FORBIDDEN forbidden}
 */
public class ForbiddenAccessException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    private Set<Supplier<String>> authorizationFailures;
    private RequestScope scope;

    public ForbiddenAccessException(String verboseMessage, RequestScope scope) {
        super(null, verboseMessage);
        this.scope = scope;
    }

    public String getReason() {
        Set<String> uniqueReasons = new HashSet<>();
        StringBuffer buf = new StringBuffer();
        buf.append("Failed authorization checks:\n");
        for (Supplier<String> authorizationFailure : scope.getFailedAuthorizations()) {
            String reason = authorizationFailure.get();
            if (!uniqueReasons.contains(reason)) {
                buf.append(authorizationFailure.get());
                buf.append("\n");
                uniqueReasons.add(reason);
            }
        }
        return buf.toString();
    }

    @Override
    public int getStatus() {
        return HttpStatus.SC_FORBIDDEN;
    }
}
