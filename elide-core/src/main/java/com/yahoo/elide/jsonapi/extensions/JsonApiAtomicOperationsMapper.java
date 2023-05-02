/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.extensions;

import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Operations;
import com.yahoo.elide.jsonapi.models.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JsonApiAtomicOperationsMapper.
 */
public class JsonApiAtomicOperationsMapper {
    private final ObjectMapper objectMapper;

    public JsonApiAtomicOperationsMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Operations readDoc(String operationsDoc) throws JsonProcessingException {
        return this.objectMapper.readValue(operationsDoc, Operations.class);
    }

    public Resource readResource(JsonNode resource) throws JsonProcessingException {
        return objectMapper.treeToValue(resource, Resource.class);
    }

    public JsonApiDocument readData(JsonNode data) throws JsonProcessingException {
        JsonApiDocument value = new JsonApiDocument();
        if (data != null) {
            if (data.isArray()) {
                List<Resource> dataResources = new ArrayList<>();
                for (JsonNode item : data) {
                    dataResources.add(readResource(item));
                    value.setData(new Data<Resource>(dataResources));
                }
            } else {
                Resource resource = readResource(data);
                value.setData(new Data<Resource>(resource));
            }
        }
        return value;
    }
}
