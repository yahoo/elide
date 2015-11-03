/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.AbstractCollection;

/**
 * Custom serializer for top-level data.
 */
public class SingletonSerializer extends JsonSerializer<AbstractCollection> {

    @Override
    public void serialize(AbstractCollection data, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException {
        jsonGenerator.writeObject(data.iterator().next());
    }
}
