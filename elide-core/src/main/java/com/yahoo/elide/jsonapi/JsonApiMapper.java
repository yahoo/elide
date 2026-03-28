/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.ElideMapper;
import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperationsMapper;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatchMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.serialization.JsonApiModule;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serializer/Deserializer for JSON API.
 */
public class JsonApiMapper {
    protected final ElideMapper elideMapper;
    protected final JsonApiJsonPatchMapper jsonPatchMapper;
    protected final JsonApiAtomicOperationsMapper atomicOperationsMapper;

    /**
     * Instantiates a new Json Api Mapper.
     */
    public JsonApiMapper() {
        this(new ElideMapper(JsonMapper.shared()));
    }

    /**
     * Instantiates a new Json Api Mapper.
     */
    public JsonApiMapper(ElideMapper elideMapper) {
        this(elideMapper, new JsonApiJsonPatchMapper(elideMapper), new JsonApiAtomicOperationsMapper(elideMapper));
    }

    /**
     * Instantiates a new Json Api Mapper.
     *
     * @param elideMapper Custom object mapper to use internally for serializing/deserializing
     * @param jsonPatchMapper the mapper for the JSON Patch extension
     * @param atomicOperationsMapper the mapper for the Atomic Operations extension
     */
    public JsonApiMapper(ElideMapper elideMapper, JsonApiJsonPatchMapper jsonPatchMapper,
            JsonApiAtomicOperationsMapper atomicOperationsMapper) {
        this.elideMapper = elideMapper;
        this.jsonPatchMapper = jsonPatchMapper;
        this.atomicOperationsMapper = atomicOperationsMapper;
        this.elideMapper.customizeObjectMapper(builder -> builder.addModule(new JsonApiModule()));
    }

    /**
     * To json object.
     *
     * @param jsonApiDocument the json api document
     * @return the json node
     */
    public JsonNode toJsonObject(JsonApiDocument jsonApiDocument) {
        return elideMapper.getObjectMapper().convertValue(jsonApiDocument, JsonNode.class);
    }

    /**
     * Write json api document.
     *
     * @param doc the document
     * @param <T> The type of document object so serialize
     * @return the string
     */
    public <T> String writeJsonApiDocument(T doc) {
        return elideMapper.getObjectMapper().writeValueAsString(doc);
    }

    /**
     * Read json api document.
     *
     * @param doc the doc
     * @return the json api document
     */
    public JsonApiDocument readJsonApiDocument(String doc) {
        JsonNode node = elideMapper.getObjectMapper().readTree(doc);
        return readJsonApiDocument(node);
    }

    /**
     * Read json api document.
     *
     * @param node the node
     * @return the json api document
     */
    public JsonApiDocument readJsonApiDocument(JsonNode node) {
        return elideMapper.getObjectMapper().treeToValue(node, JsonApiDocument.class);
    }

    /**
     * Gets the Jackson ObjectMapper.
     *
     * @return the Jackson ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return elideMapper.getObjectMapper();
    }

    /**
     * Gets the mapper for the JSON Patch extension.
     *
     * @return the mapper for the JSON Patch extension.
     */
    public JsonApiJsonPatchMapper forJsonPatch() {
        return this.jsonPatchMapper;
    }

    /**
     * Gets the mapper for the Atomic Operations extension.
     *
     * @return the mapper for the Atomic Operations extension.
     */
    public JsonApiAtomicOperationsMapper forAtomicOperations() {
        return this.atomicOperationsMapper;
    }
}
