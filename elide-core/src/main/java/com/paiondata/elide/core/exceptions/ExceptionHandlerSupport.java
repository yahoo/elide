/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;
import com.paiondata.elide.ElideResponse;

/**
 * Base implementation of an {@link ExceptionHandler}.
 *
 * @param <C> the error context type
 */
public abstract class ExceptionHandlerSupport<C extends ErrorContext> implements ExceptionHandler<C> {
    protected final ExceptionLogger exceptionLogger;
    protected final ExceptionMappers exceptionMappers;

    protected ExceptionHandlerSupport(ExceptionLogger exceptionLogger, ExceptionMappers exceptionMappers) {
        this.exceptionLogger = exceptionLogger;
        this.exceptionMappers = exceptionMappers;
    }

    @Override
    public ElideResponse<?> handleException(Throwable exception, C errorContext) {
        this.exceptionLogger.log(exception);

        if (this.exceptionMappers != null) {
            ElideErrorResponse<?> errorResponse = this.exceptionMappers.toErrorResponse(exception, errorContext);
            if (errorResponse != null) {
                return buildResponse(errorResponse);
            }
        }
        if (exception instanceof RuntimeException e) {
            return handleRuntimeException(e, errorContext);
        } else if (exception instanceof Exception e) {
            return handleNonRuntimeException(e, errorContext);
        }
        return handleThrowable(exception, errorContext);
    }

    /**
     * Builds a response from an error response.
     * <p>
     * This can be used to translate errors to the specific type expected from the API.
     *
     * @param errorResponse the error response
     * @return the response
     */
    protected abstract ElideResponse<?> buildResponse(ElideErrorResponse<?> errorResponse);

    protected ElideResponse<?> buildResponse(HttpStatusException exception, C errorContext) {
        ElideErrorResponse<?> errorResponse = (errorContext.isVerbose() ? exception.getVerboseErrorResponse()
                : exception.getErrorResponse());
        return buildResponse(errorResponse);
    }
    protected abstract ElideResponse<?> buildResponse(int status, Object body);
    protected abstract ElideResponse<?> handleRuntimeException(RuntimeException exception, C errorContext);
    protected abstract ElideResponse<?> handleNonRuntimeException(Exception exception, C errorContext);

    protected ElideResponse<?> handleThrowable(Throwable exception, ErrorContext errorContext) {
        return ElideResponse.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
    }
}
