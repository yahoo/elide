/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.jsonapi.models.Meta;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

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
    void shouldDeserialize() {
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
