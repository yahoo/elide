/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.models;

import static graphql.Assert.assertNotNull;

import com.paiondata.elide.graphql.serialization.GraphQLErrorSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import graphql.ErrorClassification;
import graphql.language.SourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GraphQL Error.
 */
public class GraphQLError {
    private GraphQLError() {
    }

    public static GraphQLErrorBuilder builder() {
        return new GraphQLErrorBuilder();
    }

    public static class GraphQLErrorBuilder extends graphql.GraphqlErrorBuilder<GraphQLErrorBuilder> {
        private Map<String, Object> extensions = new LinkedHashMap<>();
        private List<SourceLocation> locations = null;

        public GraphQLErrorBuilder path(Object ... path) {
            return path(Arrays.asList(path));
        }

        public GraphQLErrorBuilder location(Consumer<SourceLocationBuilder> location) {
            SourceLocationBuilder builder = new SourceLocationBuilder();
            location.accept(builder);
            if (getLocations() == null) {
                this.locations = new ArrayList<>();
            }
            return location(builder.build());
        }

        public GraphQLErrorBuilder locations(Consumer<List<SourceLocation>> locations) {
            if (getLocations() == null) {
                this.locations = new ArrayList<>();
            }
            locations.accept(getLocations());
            return this;
        }

        public GraphQLErrorBuilder extension(String key, Object value) {
            this.extensions.put(key, value);
            return super.extensions(this.extensions);
        }

        public GraphQLErrorBuilder extensions(Consumer<Map<String, Object>> extensions) {
            extensions.accept(this.extensions);
            return super.extensions(this.extensions);
        }

        @Override
        public GraphQLErrorBuilder extensions(Map<String, Object> extensions) {
            this.extensions = extensions;
            return super.extensions(this.extensions);
        }

        @Override
        public GraphQLErrorBuilder locations(List<SourceLocation> locations) {
            if (getLocations() == null) {
                this.locations = new ArrayList<>();
            }
            this.locations.addAll(locations);
            return this;
        }

        @Override
        public GraphQLErrorBuilder location(SourceLocation location) {
            if (getLocations() == null) {
                this.locations = new ArrayList<>();
            }
            this.locations.add(location);
            return this;
        }

        @Override
        public List<SourceLocation> getLocations() {
            return this.locations;
        }

        @Override
        public graphql.GraphQLError build() {
            assertNotNull(getMessage(), () -> "You must provide error message");
            return new BasicGraphQLError(getMessage(), locations, getErrorType(), getPath(),
                    extensions.isEmpty() ? null : extensions);
        }

        @JsonSerialize(using = GraphQLErrorSerializer.class)
        private static class BasicGraphQLError implements graphql.GraphQLError {
            private static final long serialVersionUID = 1L;
            private final String message;
            private final List<SourceLocation> locations;
            private final ErrorClassification errorType;
            private final List<Object> path;
            private final Map<String, Object> extensions;

            public BasicGraphQLError(String message, List<SourceLocation> locations, ErrorClassification errorType,
                    List<Object> path, Map<String, Object> extensions) {
                this.message = message;
                this.locations = locations;
                this.errorType = errorType;
                this.path = path;
                this.extensions = extensions;
            }

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public List<SourceLocation> getLocations() {
                return locations;
            }

            @Override
            public ErrorClassification getErrorType() {
                return errorType;
            }

            @Override
            public List<Object> getPath() {
                return path;
            }

            @Override
            public Map<String, Object> getExtensions() {
                return extensions;
            }

            @Override
            public String toString() {
                return message;
            }
        }
    }
}
