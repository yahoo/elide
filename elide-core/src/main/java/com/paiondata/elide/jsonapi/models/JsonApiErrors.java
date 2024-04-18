/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Error Objects, see http://jsonapi.org/format/#error-objects. <br><br>
 * Builder example: <br>
 * <pre><code>
 * ErrorObjects errors = ErrorObjects.builder()
 *         .error(error -> error.status("403").source(source -> source.pointer("/data/attributes/secretPowers"))
 *                 .detail("Editing secret powers is not authorized on Sundays."))
 *         .error(error -> error.status("422").source(source -> source.pointer("/data/attributes/volume"))
 *                 .detail("Volume does not, in fact, go to 11."))
 *         .error(error -> error.status("500").source(source -> source.pointer("/data/attributes/reputation"))
 *                 .title("The backend responded with an error")
 *                 .detail("Reputation service not responding after three requests."))
 *         .build();
 * </code></pre>
 */
public class JsonApiErrors {
    @Getter
    private final List<JsonApiError> errors;

    @JsonCreator
    public JsonApiErrors(@JsonProperty("errors") List<JsonApiError> errors) {
        this.errors = Objects.requireNonNull(errors, "errors must not be null");
    }

    /**
     * Returns a mutable builder for {@link JsonApiErrors}.
     *
     * @return the mutable builder
     */
    public static JsonApiErrorsBuilder builder() {
        return new JsonApiErrorsBuilder();
    }

    /**
     * The mutable builder for {@link JsonApiErrors}.
     */
    public static class JsonApiErrorsBuilder {
        private List<JsonApiError> errors = new ArrayList<>();

        public JsonApiErrorsBuilder error(JsonApiError error) {
            this.errors.add(error);
            return this;
        }

        public JsonApiErrorsBuilder error(Consumer<JsonApiError.JsonApiErrorBuilder> error) {
            JsonApiError.JsonApiErrorBuilder builder = JsonApiError.builder();
            error.accept(builder);
            return error(builder.build());
        }

        public JsonApiErrorsBuilder errors(List<JsonApiError> errors) {
            this.errors = errors;
            return this;
        }

        public JsonApiErrorsBuilder errors(Consumer<List<JsonApiError>> errors) {
            errors.accept(this.errors);
            return this;
        }

        public JsonApiErrors build() {
            if (this.errors.isEmpty()) {
                throw new IllegalArgumentException("At least one error is required");
            }
            return new JsonApiErrors(this.errors);
        }
    }
}
