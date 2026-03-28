/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.serialization;

import graphql.ExecutionResult;
import graphql.GraphQLError;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.List;
import java.util.Map;

/**
 * Custom serializer used to generate response messages. Should closely mimic the
 * {@link ExecutionResult#toSpecification()} function which specifies which fields should be present in the result of
 * a GraphQL call. {@link GraphQLError} objects should be handed off to a {@link GraphQLErrorSerializer} to handle
 * optional encoding of the error message by having the {@link GraphQLErrorSerializer} registered on the ObjectMapper.
 */
public class ExecutionResultSerializer extends StdSerializer<ExecutionResult> {
    public ExecutionResultSerializer() {
        super(ExecutionResult.class);
    }

    @Override
    public void serialize(ExecutionResult value, JsonGenerator gen, SerializationContext provider) {
        // mimic the ExecutionResult.toSpecification response
        gen.writeStartObject();
        Map<String, Object> spec = value.toSpecification();
        if (spec.containsKey("data")) {
            gen.writeName("data");
            gen.writePOJO(spec.get("data"));
        }

        if (spec.containsKey("errors")) {
            List<GraphQLError> errors = value.getErrors();
            gen.writeName("errors");
            gen.writeStartArray();
            for (GraphQLError error : errors) {
                gen.writePOJO(error);
            }
            gen.writeEndArray();
        }

        if (spec.containsKey("extensions")) {
            gen.writeName("extensions");
            gen.writePOJO(spec.get("extensions"));
        }

        gen.writeEndObject();
    }
}
