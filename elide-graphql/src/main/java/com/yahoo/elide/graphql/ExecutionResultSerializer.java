/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Custom serializer used to generate response messages. Should closely mimic the
 * {@link ExecutionResult#toSpecification()} function which specifies which fields should be present in the result of
 * a GraphQL call. {@link GraphQLError} objects should be handed off to a {@link GraphQLErrorSerializer} to handle
 * optional encoding of the error message.
 */
@Slf4j
public class ExecutionResultSerializer extends StdSerializer<ExecutionResult> {
    private final GraphQLErrorSerializer errorSerializer;

    public ExecutionResultSerializer() {
        this(new GraphQLErrorSerializer());
    }

    public ExecutionResultSerializer(GraphQLErrorSerializer errorSerializer) {
        super(ExecutionResult.class);
        this.errorSerializer = errorSerializer;
    }

    @Override
    public void serialize(ExecutionResult value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // mimic the ExecutionResult.toSpecification response
        gen.writeStartObject();
        Map<String, Object> spec = value.toSpecification();
        if (spec.containsKey("data")) {
            gen.writeObjectField("data", spec.get("data"));
        }

        if (spec.containsKey("errors")) {
            List<GraphQLError> errors = value.getErrors();
            gen.writeArrayFieldStart("errors");
            for (GraphQLError error : errors) {
                // includes start object and end object
                errorSerializer.serialize(error, gen, provider);
            }
            gen.writeEndArray();
        }

        if (spec.containsKey("extensions")) {
            gen.writeObjectField("extensions", spec.get("extensions"));
        }

        gen.writeEndObject();
    }
}
