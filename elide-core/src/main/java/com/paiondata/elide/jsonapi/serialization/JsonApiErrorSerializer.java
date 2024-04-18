/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.serialization;

import com.paiondata.elide.jsonapi.models.JsonApiError;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.owasp.encoder.Encode;

import java.io.IOException;
import java.util.Map;

/**
 * JSON API Error Serializer.
 */
public class JsonApiErrorSerializer extends StdSerializer<JsonApiError> {
    private static final long serialVersionUID = 1L;

    public JsonApiErrorSerializer() {
        super(JsonApiError.class);
    }

    @Override
    public void serialize(JsonApiError jsonApiError, JsonGenerator jsonGenerator, SerializerProvider provider)
            throws IOException {
        jsonGenerator.writeStartObject();
        writeStringField(jsonGenerator, "id", jsonApiError.getId());
        if (jsonApiError.getLinks() != null) {
            jsonGenerator.writeObjectField("links", jsonApiError.getLinks());
        }
        writeStringField(jsonGenerator, "status", jsonApiError.getStatus());
        writeStringField(jsonGenerator, "code", jsonApiError.getCode());
        if (jsonApiError.getSource() != null) {
            jsonGenerator.writeObjectField("source", jsonApiError.getSource());
        }
        writeStringField(jsonGenerator, "title", jsonApiError.getTitle());
        if (jsonApiError.getDetail() != null && !jsonApiError.getDetail().isBlank()) {
            writeStringField(jsonGenerator, "detail", Encode.forHtml(jsonApiError.getDetail()));
        }
        if (jsonApiError.getMeta() instanceof Map<?, ?> map) {
            if (!map.isEmpty()) {
                jsonGenerator.writeObjectField("meta", jsonApiError.getMeta());
            }
        } else if (jsonApiError.getMeta() != null) {
            jsonGenerator.writeObjectField("meta", jsonApiError.getMeta());
        }

        jsonGenerator.writeEndObject();
    }

    private void writeStringField(JsonGenerator jsonGenerator, String fieldName, String value) throws IOException {
        if (value != null && !value.isBlank()) {
            jsonGenerator.writeStringField(fieldName, value);
        }
    }
}
