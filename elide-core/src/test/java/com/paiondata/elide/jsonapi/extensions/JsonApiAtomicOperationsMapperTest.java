/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.models.Operations;
import com.paiondata.elide.jsonapi.models.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

/**
 * Tests for JsonApiAtomicOperationsMapper.
 */
class JsonApiAtomicOperationsMapperTest {

    @Test
    void readDataCollection() throws JsonProcessingException {
        JsonApiAtomicOperationsMapper mapper = new JsonApiAtomicOperationsMapper(new ObjectMapper());
        String operationsDoc = """
               {
                  "atomic:operations": [{
                    "op": "update",
                    "ref": {
                      "type": "articles",
                      "id": "1",
                      "relationship": "tags"
                    },
                    "data": [
                      { "type": "tags", "id": "2" },
                      { "type": "tags", "id": "3" }
                    ]
                  }]
                }
                            """;
        Operations operations = mapper.readDoc(operationsDoc);
        JsonApiDocument document = mapper.readData(operations.getOperations().get(0).getData());
        assertEquals(2, document.getData().get().size());
    }

    @Test
    void readDataSingleId() throws JsonProcessingException {
        JsonApiAtomicOperationsMapper mapper = new JsonApiAtomicOperationsMapper(new ObjectMapper());
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "update",
                    "ref": {
                      "type": "articles",
                      "id": "13",
                      "relationship": "author"
                    },
                    "data": {
                      "type": "people",
                      "id": "9"
                    }
                  }]
                }
                               """;
        Operations operations = mapper.readDoc(operationsDoc);
        JsonApiDocument document = mapper.readData(operations.getOperations().get(0).getData());
        assertEquals(1, document.getData().get().size());
        Resource resource = document.getData().get().iterator().next();
        assertEquals("people", resource.getType());
        assertEquals("9", resource.getId());
    }

    @Test
    void readDataSingleLid() throws JsonProcessingException {
        JsonApiAtomicOperationsMapper mapper = new JsonApiAtomicOperationsMapper(new ObjectMapper());
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "update",
                    "ref": {
                      "type": "articles",
                      "lid": "13",
                      "relationship": "author"
                    },
                    "data": {
                      "type": "people",
                      "lid": "9"
                    }
                  }]
                }
                               """;
        Operations operations = mapper.readDoc(operationsDoc);
        JsonApiDocument document = mapper.readData(operations.getOperations().get(0).getData());
        assertEquals(1, document.getData().get().size());
        Resource resource = document.getData().get().iterator().next();
        assertEquals("people", resource.getType());
        assertEquals("9", resource.getId());
    }

    @Test
    void readDataNull() throws JsonProcessingException {
        JsonApiAtomicOperationsMapper mapper = new JsonApiAtomicOperationsMapper(new ObjectMapper());
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "update",
                    "ref": {
                      "type": "articles",
                      "id": "13",
                      "relationship": "author"
                    },
                    "data": null
                  }]
                }
                               """;
        Operations operations = mapper.readDoc(operationsDoc);
        JsonApiDocument document = mapper.readData(operations.getOperations().get(0).getData());
        assertEquals(0, document.getData().get().size());
    }

    @Test
    void readNull() throws JsonProcessingException {
        JsonApiAtomicOperationsMapper mapper = new JsonApiAtomicOperationsMapper(new ObjectMapper());
        String operationsDoc = """
                {
                  "atomic:operations": [{
                    "op": "update",
                    "ref": {
                      "type": "articles",
                      "id": "13",
                      "relationship": "author"
                    }
                  }]
                }
                               """;
        Operations operations = mapper.readDoc(operationsDoc);
        JsonApiDocument document = mapper.readData(operations.getOperations().get(0).getData());
        assertNull(document.getData());
    }
}
