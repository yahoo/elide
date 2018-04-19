/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Error Objects, see http://jsonapi.org/format/#error-objects. <br><br>
 * Builder example: <br>
 * <pre><code>
 *   ErrorObjects errorObjects = ErrorObjects.create()
 *       .addError()
 *           .withDetail("first error message")
 *       .addError()
 *           .withDetail("second error message")
 *       .build();
 * </code></pre>
 */
public class ErrorObjects {
    @Getter private final List<Map<String, Object>> errors;

    public ErrorObjects(List<Map<String, Object>> errors) {
        this.errors = Objects.requireNonNull(errors, "errors must not be null");
    }

    public static ErrorObjectsBuilder create() {
        return new ErrorObjectsBuilder(new ErrorObjects(new ArrayList<>()));
    }

    /**
     * ErrorObjectsBuilder.
     */
    public static class ErrorObjectsBuilder {

        private final ErrorObjects container;
        private final List<Map<String, Object>> errors;

        ErrorObjectsBuilder(ErrorObjects container) {
            this.container = container;
            this.errors = container.getErrors();
        }

        public ErrorObjectBuilder addError() {
            Map<String, Object> errorObject = new HashMap<>();
            errors.add(errorObject);
            return new ErrorObjectBuilder(this, errorObject);
        }

        public ErrorObjects build() {
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("at least one error object");
            }
            return container;
        }

        /**
         * ErrorObjectBuilder.
         */
        public static class ErrorObjectBuilder {

            private ErrorObjectsBuilder rootBuilder;
            private Map<String, Object> errorObject;

            ErrorObjectBuilder(ErrorObjectsBuilder rootBuilder, Map<String, Object> errorObject) {
                this.rootBuilder = rootBuilder;
                this.errorObject = errorObject;
            }

            public ErrorObjectBuilder withId(String id) {
                errorObject.put("id", id);
                return this;
            }

            public ErrorObjectBuilder withStatus(String status) {
                errorObject.put("status", status);
                return this;
            }

            public ErrorObjectBuilder withCode(String code) {
                errorObject.put("code", code);
                return this;
            }

            public ErrorObjectBuilder withTitle(String title) {
                errorObject.put("title", title);
                return this;
            }

            public ErrorObjectBuilder withDetail(String detail) {
                errorObject.put("detail", detail);
                return this;
            }

            public ErrorObjectBuilder put(String key, Object value) {
                errorObject.put(key, value);
                return this;
            }

            public ErrorObjectBuilder addError() {
                if (errorObject.isEmpty()) {
                    throw new IllegalArgumentException("empty error object is not allow");
                }
                return rootBuilder.addError();
            }

            public ErrorObjects build() {
                if (errorObject.isEmpty()) {
                    throw new IllegalArgumentException("empty error object is not allow");
                }
                return rootBuilder.build();
            }
        }
    }
}
