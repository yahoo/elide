/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * GraphQLErrors.
 *
 * @see <a href="https://spec.graphql.org/October2021/#sec-Errors">Errors</a>
 */
public class GraphQLErrors {
    @Getter
    private final List<graphql.GraphQLError> errors;

    @JsonCreator
    public GraphQLErrors(@JsonProperty("errors") List<graphql.GraphQLError> errors) {
        this.errors = Objects.requireNonNull(errors, "errors must not be null");
    }

    /**
     * Returns a mutable builder for {@link GraphQLErrors}.
     *
     * @return
     */
    public static GraphQLErrorsBuilder builder() {
        return new GraphQLErrorsBuilder();
    }

    /**
     * The mutable builder for {@link GraphQLErrors}.
     */
    public static class GraphQLErrorsBuilder {
        private List<graphql.GraphQLError> errors = new ArrayList<>();

        public GraphQLErrorsBuilder error(graphql.GraphQLError ... error) {
            this.errors.addAll(Arrays.asList(error));
            return this;
        }

        public GraphQLErrorsBuilder error(Consumer<GraphQLError.GraphQLErrorBuilder> error) {
            GraphQLError.GraphQLErrorBuilder builder = GraphQLError.builder();
            error.accept(builder);
            return error(builder.build());
        }

        public GraphQLErrorsBuilder errors(List<graphql.GraphQLError> errors) {
            this.errors = errors;
            return this;
        }

        public GraphQLErrorsBuilder errors(Consumer<List<graphql.GraphQLError>> errors) {
            errors.accept(this.errors);
            return this;
        }

        public GraphQLErrors build() {
            if (this.errors.isEmpty()) {
                throw new IllegalArgumentException("At least one error is required");
            }
            return new GraphQLErrors(this.errors);
        }
    }
}
