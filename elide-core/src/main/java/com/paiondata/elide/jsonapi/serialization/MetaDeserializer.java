/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.serialization;

import com.paiondata.elide.jsonapi.models.Meta;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Map;

/**
 * Custom deserializer for top-level meta object.
 */
public class MetaDeserializer extends JsonDeserializer<Meta> {
    @SuppressWarnings("unchecked")
    @Override
    public Meta deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        // Optional top-level meta member must be an object
        return node.isObject() ? new Meta(jsonParser.getCodec().treeToValue(node, Map.class)) : null;
    }
}
