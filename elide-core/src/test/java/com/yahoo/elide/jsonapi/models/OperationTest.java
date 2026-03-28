/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for Operation.
 */
class OperationTest {

    @Test
    void write() {
        ObjectMapper objectMapper = new ObjectMapper();
        Ref ref = new Ref("articles", "13", null, null);
        Operation operation = new Operation(Operation.OperationCode.ADD, ref, null, null, null);
        String json = objectMapper.writeValueAsString(operation);
        String expected = """
                {"op":"add","ref":{"type":"articles","id":"13"}}""";
        assertEquals(expected, json);
    }

    @Test
    void readSingle() {
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
    void readSingleReadEnumsUsingToString() {
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
        ObjectMapper objectMapper = JsonMapper.builder().enable(EnumFeature.READ_ENUMS_USING_TO_STRING).build();
        Operation operation = objectMapper.readValue(json, Operation.class);
        assertEquals(Operation.OperationCode.UPDATE, operation.getOperationCode());
        Resource resource = objectMapper.treeToValue(operation.getData(), Resource.class);
        assertEquals("articles", resource.getType());
    }
}
