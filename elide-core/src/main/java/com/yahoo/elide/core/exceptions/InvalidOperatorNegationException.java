/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

/**
 * Invalid predicate negation error.
 *
 * {@link HttpStatus#SC_INTERNAL_SERVER_ERROR invalid}
 */
public class InvalidOperatorNegationException extends HttpStatusException {
    public InvalidOperatorNegationException() {
        super(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Invalid usage of NOT in FilterExpression.");
    }
}
