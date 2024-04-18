/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import com.paiondata.elide.core.exceptions.HttpStatus;

/**
 * Elide Response.
 * <p>
 * Builder example:
 * <pre><code>
 * ElideResponse.status(400)
 *     .body(body)
 *     .build();
 * </code></pre>
 *
 * @param <T> the body type
 */
public class ElideResponse<T> {
    /**
     * The HTTP status code.
     */
    private final int status;

    /**
     * The body.
     */
    private final T body;

    /**
     * Constructor.
     *
     * @param status HTTP response status
     * @param body the body
     */
    public ElideResponse(int status, T body) {
        this.status = status;
        this.body = body;
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return the HTTP status code of the response
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * Returns the body of the response.
     *
     * @return the body of the response
     */
    public T getBody() {
        return this.body;
    }

    /**
     * Returns the body of the response if it is of the appropriate type.
     *
     * @param <V> the expected type of the response
     * @param clazz the expected class of the response
     * @return the body of the response
     */
    public <V> V getBody(Class<V> clazz) {
        if (clazz.isInstance(this.body)) {
            return clazz.cast(this.body);
        }
        return null;
    }

    /**
     * Builds a response with this HTTP status code.
     *
     * @param status the HTTP status code
     * @return the builder
     */
    public static ElideResponseBuilder status(int status) {
        return new ElideResponseBuilder(status);
    }

    /**
     * Build a response with 200.
     *
     * @return the builder
     */
    public static ElideResponseBuilder ok() {
        return status(HttpStatus.SC_OK);
    }

    /**
     * Build a response with 200.
     *
     * @param <T> the body type
     * @param body the body
     * @return the response
     */
    public static <T> ElideResponse<T> ok(T body) {
        return ok().body(body);
    }

    /**
     * Build a response with 400.
     *
     * @return the builder
     */
    public static ElideResponseBuilder badRequest() {
        return status(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Build a response with 400.
     *
     * @param <T> the body type
     * @param body the body
     * @return the response
     */
    public static <T> ElideResponse<T> badRequest(T body) {
        return badRequest().body(body);
    }

    /**
     * Builder for building a {@link ElideResponse}.
     */
    public static class ElideResponseBuilder {
        protected int status;

        public ElideResponseBuilder(int status) {
            this.status = status;
        }

        /**
         * Sets the body of the response.
         *
         * @param <T> the body type
         * @param body the body
         * @return the response
         */
        public <T> ElideResponse<T> body(T body) {
            return new ElideResponse<>(status, body);
        }

        /**
         * Build the response with no body.
         *
         * @param <T> the body type
         * @return the response
         */
        public <T> ElideResponse<T> build() {
            return new ElideResponse<>(status, null);
        }
    }
}
