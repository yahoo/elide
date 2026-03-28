/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.serialization;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Deserializes JSON into an Execution Result.
 */
public class ExecutionResultDeserializer extends StdDeserializer<ExecutionResult> {
    public ExecutionResultDeserializer() {
        super(ExecutionResult.class);
    }

    @Override
    public ExecutionResult deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode root = context.readTree(parser);

        JsonNode dataNode = root.get("data");
        JsonNode errorsNode = root.get("errors");

        List<GraphQLError> errors = null;

        if (errorsNode != null) {
            errors = new ArrayList<>();
            Iterator<JsonNode> nodeIterator = errorsNode.iterator();
            while (nodeIterator.hasNext()) {
                JsonNode errorNode = nodeIterator.next();
                errors.add(context.readTreeAsValue(errorNode, GraphQLError.class));
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = context.readTreeAsValue(dataNode, Map.class);

        return ExecutionResultImpl.newExecutionResult()
                .errors(errors)
                .data(data).build();
    }
}
