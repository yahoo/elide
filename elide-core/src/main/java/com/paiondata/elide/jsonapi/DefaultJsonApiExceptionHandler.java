/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.ElideError;
import com.paiondata.elide.ElideErrorResponse;
import com.paiondata.elide.ElideErrors;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.exceptions.ExceptionHandlerSupport;
import com.paiondata.elide.core.exceptions.ExceptionLogger;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.exceptions.HttpStatusException;
import com.paiondata.elide.core.exceptions.InvalidURLException;
import com.paiondata.elide.core.exceptions.JsonApiAtomicOperationsException;
import com.paiondata.elide.core.exceptions.JsonPatchExtensionException;
import com.paiondata.elide.core.exceptions.TransactionException;
import com.paiondata.elide.jsonapi.models.JsonApiErrors;

import com.fasterxml.jackson.core.JacksonException;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Default implementation of {@link JsonApiExceptionHandler}.
 */
@Slf4j
public class DefaultJsonApiExceptionHandler extends ExceptionHandlerSupport<JsonApiErrorContext>
        implements JsonApiExceptionHandler {
    protected JsonApiErrorMapper jsonApiErrorMapper;

    public DefaultJsonApiExceptionHandler(ExceptionLogger exceptionLogger, ExceptionMappers exceptionMappers,
            JsonApiErrorMapper jsonApiErrorMapper) {
        super(exceptionLogger, exceptionMappers);
        this.jsonApiErrorMapper = jsonApiErrorMapper;
    }

    @Override
    public ElideResponse<?> handleException(Throwable exception, JsonApiErrorContext errorContext) {
        return super.handleException(exception, errorContext);
    }

    @Override
    protected ElideResponse<?> handleRuntimeException(RuntimeException exception, JsonApiErrorContext errorContext) {
        if (exception instanceof ForbiddenAccessException e) {
            return buildResponse(e, errorContext);
        }

        if (exception instanceof JsonPatchExtensionException e) {
            return buildResponse(e, errorContext);
        }

        if (exception instanceof JsonApiAtomicOperationsException e) {
            return buildResponse(e, errorContext);
        }

        if (exception instanceof HttpStatusException e) {
            return buildResponse(e, errorContext);
        }

        if (exception instanceof ParseCancellationException e) {
            return buildResponse(new InvalidURLException(e), errorContext);
        }

        if (exception instanceof ConstraintViolationException e) {
            final JsonApiErrors.JsonApiErrorsBuilder errors = JsonApiErrors.builder();
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                errors.error(error -> {
                    error.detail(constraintViolation.getMessage());
                    error.code(constraintViolation.getConstraintDescriptor().getAnnotation().annotationType()
                            .getSimpleName());
                    final String propertyPathString = constraintViolation.getPropertyPath().toString();
                    if (!propertyPathString.isEmpty()) {
                        error.source(
                                source -> source.pointer("/data/attributes/" + propertyPathString.replace(".", "/")));
                        error.meta(meta -> {
                            meta.put("type",  "ConstraintViolation");
                            meta.put("property",  propertyPathString);
                        });
                    }
                });
            }
            return buildResponse(HttpStatus.SC_BAD_REQUEST, errors.build());
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw exception;

    }

    @Override
    protected ElideResponse<?> handleNonRuntimeException(Exception exception, JsonApiErrorContext errorContext) {
        if (exception instanceof JacksonException jacksonException) {
            String message = (jacksonException.getLocation() != null
                    && jacksonException.getLocation().contentReference().getRawContent() != null)
                            ? exception.getMessage() //This will leak Java class info if the location isn't known.
                    : jacksonException.getOriginalMessage();

            return buildResponse(new BadRequestException(message), errorContext);
        }

        if (exception instanceof IOException) {
            return buildResponse(new TransactionException(exception), errorContext);
        }

        log.error("Error or exception uncaught by Elide", exception);
        if (exception instanceof IOException e) {
            throw new UncheckedIOException(e);
        } else {
            throw new RuntimeException(exception);
        }
    }

    @Override
    protected ElideResponse<?> buildResponse(ElideErrorResponse<?> errorResponse) {
        if (errorResponse.getBody() instanceof ElideErrors errors) {
            JsonApiErrors.JsonApiErrorsBuilder builder = JsonApiErrors.builder();
            for (ElideError error : errors.getErrors()) {
                builder.error(jsonApiErrorMapper.toJsonApiError(error));
            }
            return buildResponse(errorResponse.getStatus(), builder.build());
        } else {
            return buildResponse(errorResponse.getStatus(), errorResponse.getBody());
        }
    }

    @Override
    protected ElideResponse<?> buildResponse(int status, Object body) {
        return new ElideResponse<>(status, body);
    }
}
