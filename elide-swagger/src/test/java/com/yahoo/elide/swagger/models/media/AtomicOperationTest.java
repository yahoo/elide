/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Test for AtomicOperation.
 */
class AtomicOperationTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * For OpenAPI 3.1 the nullable keyword is not allowed.
     */
    @Test
    void openApi31() {
        AtomicOperation atomicOperation = new AtomicOperation();
        String result = Json31.pretty(atomicOperation);
        JsonNode jsonNode = objectMapper.readTree(result);
        JsonNode data = jsonNode.at("/properties/data/anyOf/1");
        JsonNode type = data.at("/type");
        JsonNode nullable = data.at("/nullable");
        assertTrue(type.isArray());
        assertTrue(nullable.isMissingNode());
    }

    /**
     * For OpenAPI 3.0 the null type is not allowed.
     */
    @Test
    void openApi30() {
        AtomicOperation atomicOperation = new AtomicOperation();
        String result = Json.pretty(atomicOperation);
        JsonNode jsonNode = objectMapper.readTree(result);
        JsonNode data = jsonNode.at("/properties/data/anyOf/1");
        JsonNode type = data.at("/type");
        JsonNode nullable = data.at("/nullable");
        assertTrue(type.isString());
        assertTrue(nullable.isBoolean());
    }
}
