/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import com.yahoo.elide.jsonapi.models.Meta;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.util.Map;

/**
 * Custom deserializer for top-level meta object.
 */
public class MetaDeserializer extends ValueDeserializer<Meta> {
    @SuppressWarnings("unchecked")
    @Override
    public Meta deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonNode node = deserializationContext.readTree(jsonParser);
        // Optional top-level meta member must be an object
        return node.isObject() ? new Meta(deserializationContext.readTreeAsValue(node, Map.class)) : null;
    }
}
