/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.extensions;

import com.yahoo.elide.ElideMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Patch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.util.List;

/**
 * The mapper for the JSON API JSON Patch extension.
 */
public class JsonApiJsonPatchMapper {
    protected final ElideMapper elideMapper;

    public JsonApiJsonPatchMapper(ElideMapper elideMapper) {
        this.elideMapper = elideMapper;
    }

    /**
     * Read json api patch ext value.
     *
     * @param value the value
     * @return the json api document
     */
    public JsonApiDocument readValue(JsonNode value) {
        JsonNode data = JsonNodeFactory.instance.objectNode().set("data", value);
        return this.elideMapper.getObjectMapper().treeToValue(data, JsonApiDocument.class);
    }

    /**
     * Read json api patch ext doc.
     *
     * @param doc the doc
     * @return the list
     * @throws IOException the iO exception
     */
    public List<Patch> readDoc(String doc) {
        ObjectMapper objectMapper = elideMapper.getObjectMapper();
        return objectMapper.readValue(doc,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Patch.class));
    }
}
