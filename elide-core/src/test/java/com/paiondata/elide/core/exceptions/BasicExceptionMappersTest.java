/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.paiondata.elide.ElideErrorResponse;
import com.paiondata.elide.ElideErrors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolationException;

/**
 * Test for BasicExceptionMappers.
 */
class BasicExceptionMappersTest {
    public static class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException, ElideErrors> {
        @Override
        public ElideErrorResponse<ElideErrors> toErrorResponse(ConstraintViolationException exception, ErrorContext errorContext) {
            return ElideErrorResponse.status(400)
                    .errors(errors -> errors.error(error -> error.message(exception.getMessage())));
        }
    }

    @Test
    void shouldReturnNull() {
        ErrorContext errorContext = mock(ErrorContext.class);
        ExceptionMappers exceptionMappers = BasicExceptionMappers.builder()
                .register(ConstraintViolationException.class, new ConstraintViolationExceptionMapper()).build();
        ElideErrorResponse<Object> response = exceptionMappers.toErrorResponse(new NullPointerException(), errorContext);
        assertNull(response);
    }

    @Test
    void shouldReturnValue() {
        ErrorContext errorContext = mock(ErrorContext.class);
        ExceptionMappers exceptionMappers = BasicExceptionMappers.builder()
                .register(new ConstraintViolationExceptionMapper()).build();
        ElideErrorResponse<Object> response = exceptionMappers.toErrorResponse(new ConstraintViolationException("message", null), errorContext);
        assertInstanceOf(ElideErrors.class, response.getBody());
        assertEquals("message", response.getBody(ElideErrors.class).getErrors().get(0).getMessage());
    }

    @Test
    void mutate() {
        ErrorContext errorContext = mock(ErrorContext.class);
        ExceptionMappers exceptionMappers = BasicExceptionMappers.builder()
                .register(new ConstraintViolationExceptionMapper()).build();
        ExceptionMappers mutated = exceptionMappers.mutate().registrations(registrations -> registrations.clear()).build();
        ElideErrorResponse<Object> response = exceptionMappers.toErrorResponse(new ConstraintViolationException("message", null), errorContext);
        assertEquals("message", response.getBody(ElideErrors.class).getErrors().get(0).getMessage());
        response = mutated.toErrorResponse(new ConstraintViolationException("message", null), errorContext);
        assertNull(response);
    }
}
