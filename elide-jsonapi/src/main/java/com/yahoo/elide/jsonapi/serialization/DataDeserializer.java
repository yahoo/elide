/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Resource;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for top-level data.
 */
public class DataDeserializer extends ValueDeserializer<Data<Resource>> {
    @Override
    public Data<Resource> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonNode node = deserializationContext.readTree(jsonParser);
        if (node.isArray()) {
            List<Resource> resources = new ArrayList<>();
            for (JsonNode n : node) {
                Resource r = deserializationContext.readTreeAsValue(n, Resource.class);
                validateResource(jsonParser, r);
                resources.add(r);
            }
            return new Data<>(resources);
        }
        Resource resource = deserializationContext.readTreeAsValue(node, Resource.class);
        validateResource(jsonParser, resource);
        return new Data<>(resource);
    }

    private void validateResource(JsonParser jsonParser, Resource resource) {
        if (resource.getType() == null || resource.getType().isEmpty()) {
            throw DatabindException.from(jsonParser, "Resource 'type' field is missing or empty.");
        }
    }
}
