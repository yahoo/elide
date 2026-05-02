/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Set;

/**
 * JSON API Set Serializer.
 */
public class JsonApiSetSerializer extends StdSerializer<Set> {
    JsonApiSetSerializer() {
        super(Set.class);
    }

    @Override
    public void serialize(Set set, JsonGenerator jsonGenerator, SerializationContext provider) {
        jsonGenerator.writeStartArray();
        for (Object value : set) {
            jsonGenerator.writePOJO(value);
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public boolean usesObjectId() {
        return true;
    }
}
