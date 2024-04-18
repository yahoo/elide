/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.core.exceptions.BasicExceptionMappers;
import com.paiondata.elide.core.exceptions.ForbiddenAccessException;
import com.paiondata.elide.core.exceptions.InvalidApiVersionException;
import com.paiondata.elide.core.exceptions.InvalidConstraintException;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.exceptions.JsonApiAtomicOperationsException;
import com.paiondata.elide.core.exceptions.JsonPatchExtensionException;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.jsonapi.models.JsonApiError;
import com.paiondata.elide.jsonapi.models.JsonApiErrors;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Test for DefaultJsonApiExceptionHandler.
 */
class DefaultJsonApiExceptionHandlerTest {

    private DefaultJsonApiExceptionHandler jsonApiExceptionHandler = new DefaultJsonApiExceptionHandler(
            new Slf4jExceptionLogger(), BasicExceptionMappers.builder().build(), new DefaultJsonApiErrorMapper());

    enum ExceptionStatusInput {
        INVALID_API(new InvalidApiVersionException(""), JsonApiErrorContext.builder().build(), 400),
        INVALID_ENTITY_BODY(new InvalidEntityBodyException(""), JsonApiErrorContext.builder().build(), 400),
        INVALID_CONSTRAINT(new InvalidConstraintException(""), JsonApiErrorContext.builder().build(), 400),
        INVALID_ENTITY_BODY_JSON_PARSE(new InvalidEntityBodyException("", new JsonParseException("")), JsonApiErrorContext.builder().build(), 400),
        JSON_PROCESSING(JsonMappingException.from((JsonParser) null, ""), JsonApiErrorContext.builder().build(), 400),
        IO(new IOException(""), JsonApiErrorContext.builder().build(), 423),
        FORBIDDEN_ACCESS(new ForbiddenAccessException(UpdatePermission.class), JsonApiErrorContext.builder().build(), 403),
        JSON_PATCH(new JsonPatchExtensionException(403, null), JsonApiErrorContext.builder().build(), 403),
        ATOMIC_OPERATIONS(new JsonApiAtomicOperationsException(403, null), JsonApiErrorContext.builder().build(), 403),
        PARSE_CANCELLATION(new ParseCancellationException(""), JsonApiErrorContext.builder().build(), 404)
        ;

        Throwable exception;
        JsonApiErrorContext errorContext;
        int status;

        ExceptionStatusInput(Throwable exception, JsonApiErrorContext errorContext, int status) {
            this.exception = exception;
            this.errorContext = errorContext;
            this.status = status;
        }
    }

    @ParameterizedTest
    @EnumSource(ExceptionStatusInput.class)
    void exception(ExceptionStatusInput input) {
        ElideResponse<?> elideResponse = jsonApiExceptionHandler.handleException(input.exception, input.errorContext);
        assertEquals(input.status, elideResponse.getStatus());
    }

    @Test
    void webApplicationExceptionShouldRethrow() {
        JsonApiErrorContext errorContext = JsonApiErrorContext.builder().build();
        assertThrows(WebApplicationException.class,
                () -> jsonApiExceptionHandler.handleException(new WebApplicationException(), errorContext));
    }

    @Test
    void exceptionShouldBeInCause() {
        JsonApiErrorContext errorContext = JsonApiErrorContext.builder().build();
        Exception exception = new Exception();
        assertThrows(RuntimeException.class,
                () -> jsonApiExceptionHandler.handleException(exception, errorContext));
        try {
            jsonApiExceptionHandler.handleException(exception, errorContext);
        } catch (RuntimeException e) {
            assertSame(exception, e.getCause());
        }
    }

    @Test
    void errorShouldReturn500() {
        JsonApiErrorContext errorContext = JsonApiErrorContext.builder().build();
        Error error = new OutOfMemoryError();
        ElideResponse<?> response = jsonApiExceptionHandler.handleException(error, errorContext);
        assertEquals(500, response.getStatus());
    }

    @Test
    void constraintViolationException() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        TestObject testObject = new TestObject();
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        ConstraintViolationException e = new ConstraintViolationException("message", violations);
        ElideResponse<?> elideResponse = jsonApiExceptionHandler.handleException(e, JsonApiErrorContext.builder().build());
        assertEquals(400, elideResponse.getStatus());
        List<JsonApiError> errors = elideResponse.getBody(JsonApiErrors.class).getErrors();
        assertEquals(3, errors.size());
    }

    public static class TestObject {
        public static class NestedTestObject {
            @NotNull
            private String nestedNotNullField;
        }

        @NotNull
        private String notNullField;

        @Min(5)
        private int minField = 1;

        @Valid
        private NestedTestObject nestedTestObject = new NestedTestObject();
    }
}
