/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

/**
 * Tests for Operations.
 */
class OperationsTest {

    @Test
    void read() throws JsonProcessingException {
        String json = """
                {
                  "atomic:operations": [{
                    "op": "remove",
                    "ref": {
                      "type": "articles",
                      "id": "1",
                      "relationship": "comments"
                    },
                    "data": [
                      { "type": "comments", "id": "12" },
                      { "type": "comments", "id": "13" }
                    ]
                  }]
                }
                      """;
        ObjectMapper objectMapper = new ObjectMapper();
        Operations operations = objectMapper.readValue(json, Operations.class);
        assertEquals(1, operations.getOperations().size());
        Operation operation = operations.getOperations().get(0);
        assertEquals(Operation.OperationCode.REMOVE, operation.getOperationCode());
        assertEquals("articles", operation.getRef().getType());
    }
}
