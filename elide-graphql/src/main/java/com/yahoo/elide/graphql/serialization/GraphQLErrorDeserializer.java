/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.serialization;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @SuppressWarnings("unchecked")
    @Override
    public GraphQLError deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode root = context.readTree(parser);

        JsonNode messageNode = root.get("message");
        JsonNode pathNode = root.get("path");
        JsonNode sourceLocations = root.get("locations");
        JsonNode extensions = root.get("extensions");
        Map<String, Object> extensionsMap = extensions == null ? Collections.emptyMap()
                : context.readTreeAsValue(extensions, Map.class);

        return new GraphQLError() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return String.format("{ \"message\": \"%s\", \"locations\": %s, \"path\": %s}",
                        getMessage(),
                        getLocations(),
                        getPath());
            }

            @Override
            public String getMessage() {
                return messageNode == null ? null : messageNode.stringValue();
            }

            @Override
            public List<SourceLocation> getLocations() {
                if (sourceLocations != null) {
                    List<SourceLocation> result = new ArrayList<>();
                    sourceLocations.forEach(sourceLocation ->
                        result.add(new SourceLocation(
                                sourceLocation.get("line").asInt(),
                                sourceLocation.get("column").asInt()
                        ))
                    );

                    return result;
                }
                return null;
            }

            @Override
            public Map<String, Object> getExtensions() {
                return extensionsMap;
            }

            @Override
            public ErrorClassification getErrorType() {
                Object classification = extensionsMap.get("classification");
                if (classification == null) {
                    classification = "DataFetchingException";
                }
                return ErrorClassification.errorClassification(classification.toString());
            }

            @Override
            public List<Object> getPath() {
                if (pathNode != null) {
                    List<Object> paths = new ArrayList<>();
                    pathNode.forEach(path ->
                        paths.add(path.asString())
                    );
                    return paths;
                }
                return null;
            }
        };
    }
}
