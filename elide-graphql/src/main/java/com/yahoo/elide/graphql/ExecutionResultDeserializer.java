/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Deserializes JSON into an Execution Result.
 */
@Slf4j
public class ExecutionResultDeserializer extends StdDeserializer<ExecutionResult> {

    ObjectMapper mapper;
    GraphQLErrorDeserializer errorDeserializer;

    public ExecutionResultDeserializer() {
        super(ExecutionResult.class);
        mapper = new ObjectMapper();
        errorDeserializer = new GraphQLErrorDeserializer();
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
                errors.add(errorDeserializer.deserialize(errorNode.traverse(parser.getCodec()), context));
            }
        }

        Map<String, Object> data = mapper.convertValue(dataNode, new TypeReference<>() { });

        return ExecutionResultImpl.newExecutionResult()
                .errors(errors)
                .data(data).build();
    }
}
