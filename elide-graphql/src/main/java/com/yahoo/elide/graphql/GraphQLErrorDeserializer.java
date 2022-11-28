/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static graphql.ErrorType.ExecutionAborted;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializes JSON into a GraphQLError.
 */
public class GraphQLErrorDeserializer extends StdDeserializer<GraphQLError> {

    /**
     * Constructor.
     */
    public GraphQLErrorDeserializer() {
        super(GraphQLError.class);
    }

    @Override
    public GraphQLError deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode root = parser.getCodec().readTree(parser);

        JsonNode messageNode = root.get("message");
        JsonNode pathNode = root.get("path");
        JsonNode sourceLocations = root.get("locations");

        GraphQLError error = new GraphQLError() {
            @Override
            public String toString() {
                return String.format("{ \"message\": \"%s\", \"locations\": %s, \"path\": %s}",
                        getMessage(),
                        getLocations(),
                        getPath());
            }

            @Override
            public String getMessage() {
                return messageNode == null ? null : messageNode.textValue();
            }

            @Override
            public List<SourceLocation> getLocations() {
                if (sourceLocations != null) {
                    List<SourceLocation> result = new ArrayList<>();
                    sourceLocations.forEach(sourceLocation -> {
                        result.add(new SourceLocation(
                                sourceLocation.get("line").asInt(),
                                sourceLocation.get("column").asInt()
                        ));
                    });

                    return result;
                }
                return null;
            }

            @Override
            public ErrorClassification getErrorType() {
                return ExecutionAborted;
            }

            @Override
            public List<Object> getPath() {
                if (pathNode != null) {
                    List<Object> paths = new ArrayList<>();
                    pathNode.forEach(path -> {
                        paths.add(path.asText());
                    });
                    return paths;
                }
                return null;
            }
        };

        return error;
    }
}
