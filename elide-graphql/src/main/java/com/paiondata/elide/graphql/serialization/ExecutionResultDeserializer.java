/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Deserializes JSON into an Execution Result.
 */
public class ExecutionResultDeserializer extends StdDeserializer<ExecutionResult> {
    private static final long serialVersionUID = 1L;

    public ExecutionResultDeserializer() {
        super(ExecutionResult.class);
    }

    @Override
    public ExecutionResult deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode root = parser.getCodec().readTree(parser);

        JsonNode dataNode = root.get("data");
        JsonNode errorsNode = root.get("errors");

        List<GraphQLError> errors = null;

        if (errorsNode != null) {
            errors = new ArrayList<>();
            Iterator<JsonNode> nodeIterator = errorsNode.iterator();
            while (nodeIterator.hasNext()) {
                JsonNode errorNode = nodeIterator.next();
                errors.add(parser.getCodec().treeToValue(errorNode, GraphQLError.class));
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = parser.getCodec().treeToValue(dataNode, Map.class);

        return ExecutionResultImpl.newExecutionResult()
                .errors(errors)
                .data(data).build();
    }
}
