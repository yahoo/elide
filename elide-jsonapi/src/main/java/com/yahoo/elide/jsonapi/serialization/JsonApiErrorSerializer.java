/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import com.yahoo.elide.jsonapi.models.JsonApiError;

import org.owasp.encoder.Encode;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Map;

/**
 * JSON API Error Serializer.
 */
public class JsonApiErrorSerializer extends StdSerializer<JsonApiError> {

    public JsonApiErrorSerializer() {
        super(JsonApiError.class);
    }

    @Override
    public void serialize(JsonApiError jsonApiError, JsonGenerator jsonGenerator, SerializationContext provider) {
        jsonGenerator.writeStartObject();
        writeStringField(jsonGenerator, "id", jsonApiError.getId());
        if (jsonApiError.getLinks() != null) {
            jsonGenerator.writeName("links");
            jsonGenerator.writePOJO(jsonApiError.getLinks());
        }
        writeStringField(jsonGenerator, "status", jsonApiError.getStatus());
        writeStringField(jsonGenerator, "code", jsonApiError.getCode());
        if (jsonApiError.getSource() != null) {
            jsonGenerator.writeName("source");
            jsonGenerator.writePOJO(jsonApiError.getSource());
        }
        writeStringField(jsonGenerator, "title", jsonApiError.getTitle());
        if (jsonApiError.getDetail() != null && !jsonApiError.getDetail().isBlank()) {
            writeStringField(jsonGenerator, "detail", Encode.forHtml(jsonApiError.getDetail()));
        }
        if (jsonApiError.getMeta() instanceof Map<?, ?> map) {
            if (!map.isEmpty()) {
                jsonGenerator.writeName("meta");
                jsonGenerator.writePOJO(jsonApiError.getMeta());
            }
        } else if (jsonApiError.getMeta() != null) {
            jsonGenerator.writeName("meta");
            jsonGenerator.writePOJO(jsonApiError.getMeta());
        }

        jsonGenerator.writeEndObject();
    }

    private void writeStringField(JsonGenerator jsonGenerator, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            jsonGenerator.writeName(fieldName);
            jsonGenerator.writeString(value);
        }
    }
}
