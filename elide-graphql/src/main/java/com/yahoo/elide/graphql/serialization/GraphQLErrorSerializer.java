/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.serialization;

import org.owasp.encoder.Encode;

import graphql.GraphQLError;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Map;

/**
 * Custom serializer used to generate response messages from {@link GraphQLError} objects.
 * Supports encoding of the error's message field, making it safe for display in HTML.
 */
public class GraphQLErrorSerializer extends StdSerializer<GraphQLError> {
    /**
     * Construct a new GraphQLErrorSerializer, optionally with error encoding enabled.
     */
    public GraphQLErrorSerializer() {
        super(GraphQLError.class);
    }

    @Override
    public void serialize(GraphQLError value, JsonGenerator gen, SerializationContext provider) {
        Map<String, Object> errorSpec = value.toSpecification();
        gen.writeStartObject();

        gen.writeName("message");
        gen.writeString(Encode.forHtml((String) errorSpec.get("message")));

        if (errorSpec.containsKey("locations")) {
            gen.writeName("locations");
            gen.writePOJO(errorSpec.get("locations"));
        }

        if (errorSpec.containsKey("path")) {
            gen.writeName("path");
            gen.writePOJO(errorSpec.get("path"));
        }

        if (errorSpec.containsKey("extensions")) {
            gen.writeName("extensions");
            gen.writePOJO(errorSpec.get("extensions"));
        }

        gen.writeEndObject();
    }
}
