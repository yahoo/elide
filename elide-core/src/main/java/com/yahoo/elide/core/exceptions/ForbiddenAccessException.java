/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.security.permissions.expressions.Expression;
import lombok.Getter;

import java.util.Optional;

/**
 * Access to the requested resource is.
 *
 * {@link com.yahoo.elide.core.HttpStatus#SC_FORBIDDEN forbidden}
 */
public class ForbiddenAccessException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    @Getter private final Optional<Expression> expression;

    public ForbiddenAccessException(String message) {
        super(HttpStatus.SC_FORBIDDEN, null, message);
        this.expression = Optional.empty();
    }

    public ForbiddenAccessException(String message, Expression expression) {
        super(HttpStatus.SC_FORBIDDEN, null, message);
        this.expression = Optional.of(expression);
    }

    public String getLoggedMessage() {
        return String.format("ForbiddenAccessException : Message[%s] Expression [%s]",
                getVerboseMessage(),
                getExpression());
    }
}
