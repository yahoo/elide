/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.jsonapi.models.Meta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for MetaDeserializer.
 *
 */
class MetaDeserializerTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("rawtypes")
    @Test
    void shouldDeserialize() throws JsonMappingException, JsonProcessingException {
        String value = """
                {
                  "hello": "world",
                  "authors": [
                    "the",
                    "quick"
                  ],
                  "map": {
                    "key": "value"
                  }
                }
                """;

        Meta meta = objectMapper.readValue(value, Meta.class);
        assertEquals("world", meta.getValue("hello"));
        assertEquals("the", ((List) meta.getValue("authors")).get(0));
        assertEquals("quick", ((List) meta.getValue("authors")).get(1));
        assertEquals("value", ((Map) meta.getValue("map")).get("key"));
    }
}
