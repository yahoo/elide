/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.ElideError;
import com.paiondata.elide.ElideErrorResponse;
import com.paiondata.elide.ElideErrors;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.core.exceptions.ExceptionHandlerSupport;
import com.paiondata.elide.core.exceptions.ExceptionLogger;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.exceptions.HttpStatusException;
import com.paiondata.elide.core.exceptions.InvalidApiVersionException;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.exceptions.TransactionException;
import com.paiondata.elide.graphql.models.GraphQLErrors;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;

import graphql.GraphQLException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Default {@link GraphQLExceptionHandler}.
 */
@Slf4j
public class DefaultGraphQLExceptionHandler extends ExceptionHandlerSupport<GraphQLErrorContext>
        implements GraphQLExceptionHandler {
    protected GraphQLErrorMapper graphqlErrorMapper;

    public DefaultGraphQLExceptionHandler(ExceptionLogger exceptionLogger, ExceptionMappers exceptionMappers,
            GraphQLErrorMapper graphqlErrorMapper) {
        super(exceptionLogger, exceptionMappers);
        this.graphqlErrorMapper = graphqlErrorMapper;
    }

    @Override
    protected ElideResponse<?> handleRuntimeException(RuntimeException exception, GraphQLErrorContext errorContext) {
        if (exception instanceof GraphQLException e) {
            String body = e.getMessage();
            return ElideResponse.status(HttpStatus.SC_OK).body(body);
        }

        if (exception instanceof InvalidEntityBodyException e) {
            if (e.getCause() instanceof JsonParseException) {
                return buildResponse(e, errorContext);
            }
            return buildResponse(HttpStatus.SC_OK, e, errorContext);
        }

        if (exception instanceof InvalidApiVersionException e) {
            return buildResponse(e, errorContext);
        }

        if (exception instanceof HttpStatusException e) {
            return buildResponse(HttpStatus.SC_OK, e, errorContext);
        }

        if (exception instanceof ConstraintViolationException e) {
            final GraphQLErrors.GraphQLErrorsBuilder errors = GraphQLErrors.builder();
            if (e.getConstraintViolations() != null) {
                for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                    errors.error(error -> {
                        error.message(constraintViolation.getMessage());
                        error.extension("code", constraintViolation.getConstraintDescriptor().getAnnotation()
                                .annotationType().getSimpleName());
                        error.extension("type",  "ConstraintViolation");
                        final String propertyPathString = constraintViolation.getPropertyPath().toString();
                        if (!propertyPathString.isEmpty()) {
                            error.extension("property",  propertyPathString);
                        }
                    });
                }
            }
            return buildResponse(HttpStatus.SC_OK, errors.build());
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw exception;
    }

    @Override
    protected ElideResponse<?> handleNonRuntimeException(Exception exception, GraphQLErrorContext errorContext) {
        if (exception instanceof JsonProcessingException) {
            return buildResponse(new InvalidEntityBodyException(errorContext.getGraphQLDocument()), errorContext);
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
            GraphQLErrors.GraphQLErrorsBuilder builder = GraphQLErrors.builder();
            for (ElideError error : errors.getErrors()) {
                builder.error(graphqlErrorMapper.toGraphQLError(error));
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

    /**
     * GraphQL returns 200 instead of 4xx for errors.
     */
    protected ElideResponse<?> buildResponse(int status, HttpStatusException exception,
            GraphQLErrorContext errorContext) {
        return ElideResponse.status(status).body(super.buildResponse(exception, errorContext).getBody());
    }
}
