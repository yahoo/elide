/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.extensions;

import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.models.Patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.util.List;

/**
 * The mapper for the JSON API JSON Patch extension.
 */
public class JsonApiJsonPatchMapper {
    protected final ObjectMapper objectMapper;

    public JsonApiJsonPatchMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Read json api patch ext value.
     *
     * @param value the value
     * @return the json api document
     * @throws JsonProcessingException the json processing exception
     */
    public JsonApiDocument readValue(JsonNode value) throws JsonProcessingException {
        JsonNode data = JsonNodeFactory.instance.objectNode().set("data", value);
        return this.objectMapper.treeToValue(data, JsonApiDocument.class);
    }

    /**
     * Read json api patch ext doc.
     *
     * @param doc the doc
     * @return the list
     * @throws IOException the iO exception
     */
    public List<Patch> readDoc(String doc) throws IOException {
        return this.objectMapper.readValue(doc,
                this.objectMapper.getTypeFactory().constructCollectionType(List.class, Patch.class));
    }
}
