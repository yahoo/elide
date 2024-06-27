/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.models.media;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;

/**
 * Test for AtomicOperation.
 */
class AtomicOperationTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * For OpenAPI 3.1 the nullable keyword is not allowed.
     *
     * @throws JsonMappingException exception
     * @throws JsonProcessingException exception
     */
    @Test
    void openApi31() throws JsonMappingException, JsonProcessingException {
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
     *
     * @throws JsonMappingException exception
     * @throws JsonProcessingException exception
     */
    @Test
    void openApi30() throws JsonMappingException, JsonProcessingException {
        AtomicOperation atomicOperation = new AtomicOperation();
        String result = Json.pretty(atomicOperation);
        JsonNode jsonNode = objectMapper.readTree(result);
        JsonNode data = jsonNode.at("/properties/data/anyOf/1");
        JsonNode type = data.at("/type");
        JsonNode nullable = data.at("/nullable");
        assertTrue(type.isTextual());
        assertTrue(nullable.isBoolean());
    }
}
