/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.extensions;

import com.yahoo.elide.ElideMapper;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Operations;
import com.yahoo.elide.jsonapi.models.Resource;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The mapper for the JSON API Atomic Operations extension.
 */
public class JsonApiAtomicOperationsMapper {
    protected final ElideMapper elideMapper;

    public JsonApiAtomicOperationsMapper(ElideMapper elideMapper) {
        this.elideMapper = elideMapper;
    }

    public Operations readDoc(String operationsDoc) {
        return this.elideMapper.getObjectMapper().readValue(operationsDoc, Operations.class);
    }

    public Resource readResource(JsonNode resource) {
        return elideMapper.getObjectMapper().treeToValue(resource, Resource.class);
    }

    public JsonApiDocument readData(JsonNode data) {
        JsonApiDocument value = new JsonApiDocument();
        if (data != null) {
            if (data.isArray()) {
                List<Resource> dataResources = new ArrayList<>();
                for (JsonNode item : data) {
                    dataResources.add(readResource(item));
                    value.setData(new Data<>(dataResources));
                }
            } else {
                Resource resource = readResource(data);
                value.setData(new Data<>(resource));
            }
        }
        return value;
    }
}
