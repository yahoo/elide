/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Set;

/**
 * JSON API Set Serializer.
 */
public class JsonApiSetSerializer extends StdSerializer<Set> {
    private static final long serialVersionUID = 1L;

    JsonApiSetSerializer() {
        super(Set.class);
    }

    @Override
    public void serialize(Set set, JsonGenerator jsonGenerator, SerializerProvider provider)
            throws IOException {
        jsonGenerator.writeStartArray();
        for (Object value : set) {
            jsonGenerator.writeObject(value);
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public boolean usesObjectId() {
        return true;
    }
}
