/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Set;

/**
 * JSON API Serializer.
 *
 * @param <T> type supported
 */
public class JsonApiSerializer<T> extends StdSerializer<T> {

    private final Class<T> type;

    JsonApiSerializer(Class<T> type) {
        super(type);
        this.type = type;
    }

    public static Module getModule() {
        SimpleModule jsonApiModule = new SimpleModule("JsonApiModule", new Version(1, 0, 0, null, null, null));
        jsonApiModule.addSerializer(new JsonApiSerializer<>(Set.class));
        return jsonApiModule;
    }

    @Override
    public void serialize(T object, JsonGenerator jsonGenerator, SerializerProvider provider)
            throws IOException {
        if (object instanceof Set) {
            jsonGenerator.writeStartArray();
            for (Object value : (Set) object) {
                jsonGenerator.writeObject(value);
            }
            jsonGenerator.writeEndArray();
        } else {
            jsonGenerator.writeObject(object);
        }
    }

    @Override
    public boolean usesObjectId() {
        return true;
    }

    @Override
    public Class<T> handledType() {
        return this.type;
    }
}
