/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import com.paiondata.elide.ElideErrors.ElideErrorsBuilder;
import com.paiondata.elide.core.exceptions.HttpStatus;

import java.util.function.Consumer;

/**
 * Elide Error Response.
 * <p>
 * Builder example:
 * <pre><code>
 * ElideErrorResponse.status(400)
 *     .errors(errors -> errors.error(error -> error.message(message)))
 *     .build();
 * </code></pre>
 *
 * @param <T> the body type
 */
public class ElideErrorResponse<T> extends ElideResponse<T> {
    /**
     * Constructor.
     *
     * @param status HTTP response status
     * @param body the body
     */
    public ElideErrorResponse(int status, T body) {
        super(status, body);
    }

    /**
     * Builds a response with this HTTP status code.
     *
     * @param status the HTTP status code
     * @return the builder
     */
    public static ElideErrorResponseBuilder status(int status) {
        return new ElideErrorResponseBuilder(status);
    }

    /**
     * Build a response with 200.
     *
     * @return the builder
     */
    public static ElideErrorResponseBuilder ok() {
        return status(HttpStatus.SC_OK);
    }

    /**
     * Build a response with 200.
     *
     * @param <T> the body type
     * @param body the body
     * @return the response
     */
    public static <T> ElideErrorResponse<T> ok(T body) {
        return ok().body(body);
    }

    /**
     * Build a response with 400.
     *
     * @return the builder
     */
    public static ElideErrorResponseBuilder badRequest() {
        return status(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Build a response with 400.
     *
     * @param <T> the body type
     * @param body the body
     * @return the response
     */
    public static <T> ElideErrorResponse<T> badRequest(T body) {
        return badRequest().body(body);
    }

    /**
     * Builder for building a {@link ElideErrorResponse}.
     */
    public static class ElideErrorResponseBuilder extends ElideResponseBuilder {

        public ElideErrorResponseBuilder(int status) {
            super(status);
        }

        /**
         * Sets the body of the response.
         *
         * @param <T> the body type
         * @param body the body
         * @return the response
         */
        public <T> ElideErrorResponse<T> body(T body) {
            return new ElideErrorResponse<>(status, body);
        }

        /**
         * Build the response with no body.
         *
         * @param <T> the body type
         * @return the response
         */
        public <T> ElideErrorResponse<T> build() {
            return new ElideErrorResponse<>(status, null);
        }

        /**
         * Sets the body of the response to {@link ElideErrors}.
         *
         * @param errors to customize
         * @return the response
         */
        public ElideErrorResponse<ElideErrors> errors(Consumer<ElideErrorsBuilder> errors) {
            ElideErrors.ElideErrorsBuilder builder = ElideErrors.builder();
            errors.accept(builder);
            return body(builder.build());
        }
    }
}
