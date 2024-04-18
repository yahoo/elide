/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;
import com.paiondata.elide.ElideErrors;

import java.util.Objects;

/**
 * {@link RuntimeException} that can produce the verbose and basic error
 * response.
 * <p>
 * This can be extended for business exceptions.
 */
public class ErrorResponseException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    private final ElideErrors errors;

    /**
     * Constructor.
     *
     * @param status http status
     * @param message exception message
     * @param errors custom error objects, not {@code null}
     */
    public ErrorResponseException(int status, String message, ElideErrors errors) {
        this(status, message, null, errors);
    }

    /**
     * Constructor.
     *
     * @param status http status
     * @param message exception message
     * @param cause the cause
     * @param errorObjects custom error objects, not {@code null}
     */
    public ErrorResponseException(int status, String message, Throwable cause, ElideErrors errors) {
        super(status, message, cause, null);
        this.errors = Objects.requireNonNull(errors, "errors must not be null");
    }

    @Override
    public ElideErrorResponse<? extends Object> getErrorResponse() {
        return buildResponse(this.errors);
    }

    @Override
    public ElideErrorResponse<? extends Object> getVerboseErrorResponse() {
        return buildResponse(this.errors);
    }

    protected ElideErrorResponse<?> buildResponse(Object body) {
        return ElideErrorResponse.status(getStatus()).body(body);
    }
}
