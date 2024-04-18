/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.core.exceptions.BasicExceptionMappers;
import com.paiondata.elide.core.exceptions.InvalidApiVersionException;
import com.paiondata.elide.core.exceptions.InvalidConstraintException;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.graphql.models.GraphQLErrors;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import graphql.GraphQLError;
import graphql.GraphQLException;

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
 * Test for DefaultGraphQLExceptionHandler.
 */
class DefaultGraphQLExceptionHandlerTest {

    private DefaultGraphQLExceptionHandler graphqlExceptionHandler = new DefaultGraphQLExceptionHandler(
            new Slf4jExceptionLogger(), BasicExceptionMappers.builder().build(), new DefaultGraphQLErrorMapper());

    enum ExceptionStatusInput {
        GRAPHQL(new GraphQLException(""), GraphQLErrorContext.builder().build(), 200),
        INVALID_API(new InvalidApiVersionException(""), GraphQLErrorContext.builder().build(), 400),
        INVALID_ENTITY_BODY(new InvalidEntityBodyException(""), GraphQLErrorContext.builder().build(), 200),
        INVALID_CONSTRAINT(new InvalidConstraintException(""), GraphQLErrorContext.builder().build(), 200),
        INVALID_ENTITY_BODY_JSON_PARSE(new InvalidEntityBodyException("", new JsonParseException("")), GraphQLErrorContext.builder().build(), 400),
        JSON_PROCESSING(JsonMappingException.from((JsonParser) null, ""), GraphQLErrorContext.builder().build(), 400),
        IO(new IOException(""), GraphQLErrorContext.builder().build(), 423),
        ;

        Throwable exception;
        GraphQLErrorContext errorContext;
        int status;

        ExceptionStatusInput(Throwable exception, GraphQLErrorContext errorContext, int status) {
            this.exception = exception;
            this.errorContext = errorContext;
            this.status = status;
        }
    }

    @ParameterizedTest
    @EnumSource(ExceptionStatusInput.class)
    void exception(ExceptionStatusInput input) {
        ElideResponse<?> elideResponse = graphqlExceptionHandler.handleException(input.exception, input.errorContext);
        assertEquals(input.status, elideResponse.getStatus());
    }

    @Test
    void webApplicationExceptionShouldRethrow() {
        GraphQLErrorContext errorContext = GraphQLErrorContext.builder().build();
        assertThrows(WebApplicationException.class,
                () -> graphqlExceptionHandler.handleException(new WebApplicationException(), errorContext));
    }

    @Test
    void exceptionShouldBeInCause() {
        GraphQLErrorContext errorContext = GraphQLErrorContext.builder().build();
        Exception exception = new Exception();
        assertThrows(RuntimeException.class,
                () -> graphqlExceptionHandler.handleException(exception, errorContext));
        try {
            graphqlExceptionHandler.handleException(exception, errorContext);
        } catch (RuntimeException e) {
            assertSame(exception, e.getCause());
        }
    }

    @Test
    void errorShouldReturn500() {
        GraphQLErrorContext errorContext = GraphQLErrorContext.builder().build();
        Error error = new OutOfMemoryError();
        ElideResponse<?> response = graphqlExceptionHandler.handleException(error, errorContext);
        assertEquals(500, response.getStatus());
    }

    @Test
    void constraintViolationException() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        TestObject testObject = new TestObject();
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        ConstraintViolationException e = new ConstraintViolationException("message", violations);
        ElideResponse<?> elideResponse = graphqlExceptionHandler.handleException(e, GraphQLErrorContext.builder().build());
        assertEquals(200, elideResponse.getStatus());
        List<GraphQLError> errors = elideResponse.getBody(GraphQLErrors.class).getErrors();
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
