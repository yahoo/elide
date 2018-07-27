/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for top-level data.
 */
public class DataDeserializer extends JsonDeserializer<Data<Resource>> {
    private final static ObjectMapper MAPPER = new MappingJsonFactory().getCodec();

    @Override
    public Data<Resource> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node.isArray()) {
            List<Resource> resources = new ArrayList<>();
            for (JsonNode n : node) {
                Resource r = MAPPER.convertValue(n, Resource.class);
                resources.add(r);
            }
            return new Data<>(resources);
        }
        Resource resource = MAPPER.convertValue(node, Resource.class);
        return new Data<>(resource);
    }
}
