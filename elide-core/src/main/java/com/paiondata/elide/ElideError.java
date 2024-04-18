/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents an error that can later be mapped to the more specific
 * JsonApiError or GraphQLError.
 *
 * @see ElideErrors
 */
@Getter
public class ElideError {
    /**
     * The error message. For JSON-API this will be mapped to the details member and
     * for GraphQL this will be mapped to the message member.
     */
    private final String message;

    /**
     * Additional attributes about the error. For JSON-API this will be mapped to the
     * meta member and for GraphQL this will be mapped to the extensions member.
     */
    private final Map<String, Object> attributes;

    public ElideError(String message, Map<String, Object> attributes) {
        this.message = message;
        this.attributes = attributes;
    }

    /**
     * Returns a mutable {@link ElideErrorBuilder} for building {@link ElideError}.
     *
     * @return the builder
     */
    public static ElideErrorBuilder builder() {
        return new ElideErrorBuilder();
    }

    /**
     * A mutable builder for building {@link ElideError}.
     */
    public static class ElideErrorBuilder {
        private String message;
        private Map<String, Object> attributes = new LinkedHashMap<>();

        /**
         * Sets the message of the error.
         * <p>
         * For JSON-API this will be mapped to the details member and for GraphQL this
         * will be mapped to the message member.
         *
         * @param message the message
         * @return the builder
         */
        public ElideErrorBuilder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the attributes.
         *
         * @param attributes the attributes to set
         * @return the builder
         */
        public ElideErrorBuilder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Customize the attributes.
         *
         * @param attributes the customizer
         * @return the builder
         */
        public ElideErrorBuilder attributes(Consumer<Map<String, Object>> attributes) {
            attributes.accept(this.attributes);
            return this;
        }

        /**
         * Sets the attribute.
         *
         * @param key the key
         * @param value the value
         * @return the builder
         */
        public ElideErrorBuilder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        /**
         * Builds the {@link ElideError}.
         *
         * @return the ElideError
         */
        public ElideError build() {
            return new ElideError(this.message, this.attributes);
        }
    }
}
