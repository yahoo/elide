/*
 * Copyright 2019, Yahoo Inc.
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
import java.util.Map;


/**
 * Deserializes JSON into an Execution Result.
 */
@Slf4j
public class ExecutionResultDeserializer extends StdDeserializer<ExecutionResult> {

    ObjectMapper mapper;

    public ExecutionResultDeserializer() {
        super(ExecutionResult.class);
        mapper = new ObjectMapper();
    }

    @Override
    public ExecutionResult deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode root = parser.getCodec().readTree(parser);

        JsonNode dataNode = root.get("data");

        Map<String, Object> data = mapper.convertValue(dataNode, new TypeReference<Map<String, Object>>(){});

        return ExecutionResultImpl.newExecutionResult().data(data).build();
    }
}
