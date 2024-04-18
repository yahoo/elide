/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Tests for Operation.
 */
class OperationTest {

    @Test
    void write() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Ref ref = new Ref("articles", "13", null, null);
        Operation operation = new Operation(Operation.OperationCode.ADD, ref, null, null, null);
        String json = objectMapper.writeValueAsString(operation);
        String expected = """
                {"op":"add","ref":{"type":"articles","id":"13"}}""";
        assertEquals(expected, json);
    }

    @Test
    void readSingle() throws JsonProcessingException {
        String json = """
                {
                  "op": "update",
                  "data": {
                    "type": "articles",
                    "id": "13",
                    "attributes": {
                      "title": "To TDD or Not"
                    }
                  }
                }
                """;
        ObjectMapper objectMapper = new ObjectMapper();
        Operation operation = objectMapper.readValue(json, Operation.class);
        assertEquals(Operation.OperationCode.UPDATE, operation.getOperationCode());
        Resource resource = objectMapper.treeToValue(operation.getData(), Resource.class);
        assertEquals("articles", resource.getType());
    }

    @Test
    void readSingleReadEnumsUsingToString() throws JsonProcessingException {
        String json = """
                {
                  "op": "update",
                  "data": {
                    "type": "articles",
                    "id": "13",
                    "attributes": {
                      "title": "To TDD or Not"
                    }
                  }
                }
                """;
        ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        Operation operation = objectMapper.readValue(json, Operation.class);
        assertEquals(Operation.OperationCode.UPDATE, operation.getOperationCode());
        Resource resource = objectMapper.treeToValue(operation.getData(), Resource.class);
        assertEquals("articles", resource.getType());
    }
}
