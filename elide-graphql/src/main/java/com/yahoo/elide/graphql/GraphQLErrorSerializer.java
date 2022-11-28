/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.owasp.encoder.Encode;

import graphql.GraphQLError;

import java.io.IOException;
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
    public void serialize(GraphQLError value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Map<String, Object> errorSpec = value.toSpecification();
        gen.writeStartObject();

        gen.writeStringField("message", Encode.forHtml((String) errorSpec.get("message")));

        if (errorSpec.containsKey("locations")) {
            gen.writeFieldName("locations");
            gen.writeObject(errorSpec.get("locations"));
        }

        if (errorSpec.containsKey("path")) {
            gen.writeFieldName("path");
            gen.writeObject(errorSpec.get("path"));
        }

        if (errorSpec.containsKey("extensions")) {
            gen.writeFieldName("extensions");
            gen.writeObject(errorSpec.get("extensions"));
        }

        gen.writeEndObject();
    }
}
