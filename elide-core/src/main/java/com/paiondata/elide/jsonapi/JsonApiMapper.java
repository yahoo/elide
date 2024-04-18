/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.jsonapi.extensions.JsonApiAtomicOperationsMapper;
import com.paiondata.elide.jsonapi.extensions.JsonApiJsonPatchMapper;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.serialization.JsonApiModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Serializer/Deserializer for JSON API.
 */
public class JsonApiMapper {
    protected final ObjectMapper mapper;
    protected final JsonApiJsonPatchMapper jsonPatchMapper;
    protected final JsonApiAtomicOperationsMapper atomicOperationsMapper;

    /**
     * Instantiates a new Json Api Mapper.
     */
    public JsonApiMapper() {
        this(new ObjectMapper());
    }

    /**
     * Instantiates a new Json Api Mapper.
     *
     * @param mapper Custom object mapper to use internally for serializing/deserializing
     */
    public JsonApiMapper(ObjectMapper mapper) {
        this(mapper, new JsonApiJsonPatchMapper(mapper), new JsonApiAtomicOperationsMapper(mapper));
    }

    /**
     * Instantiates a new Json Api Mapper.
     *
     * @param mapper Custom object mapper to use internally for serializing/deserializing
     * @param jsonPatchMapper the mapper for the JSON Patch extension
     * @param atomicOperationsMapper the mapper for the Atomic Operations extension
     */
    public JsonApiMapper(ObjectMapper mapper, JsonApiJsonPatchMapper jsonPatchMapper,
            JsonApiAtomicOperationsMapper atomicOperationsMapper) {
        this.mapper = mapper;
        this.mapper.registerModule(new JsonApiModule());
        this.jsonPatchMapper = jsonPatchMapper;
        this.atomicOperationsMapper = atomicOperationsMapper;
    }

    /**
     * To json object.
     *
     * @param jsonApiDocument the json api document
     * @return the json node
     */
    public JsonNode toJsonObject(JsonApiDocument jsonApiDocument) {
        return mapper.convertValue(jsonApiDocument, JsonNode.class);
    }

    /**
     * Write json api document.
     *
     * @param doc the document
     * @param <T> The type of document object so serialize
     * @return the string
     * @throws JsonProcessingException the json processing exception
     */
    public <T> String writeJsonApiDocument(T doc) throws JsonProcessingException {
        return mapper.writeValueAsString(doc);
    }

    /**
     * Read json api document.
     *
     * @param doc the doc
     * @return the json api document
     * @throws IOException the iO exception
     */
    public JsonApiDocument readJsonApiDocument(String doc) throws IOException {
        JsonNode node = mapper.readTree(doc);
        return readJsonApiDocument(node);
    }

    /**
     * Read json api document.
     *
     * @param node the node
     * @return the json api document
     * @throws IOException the iO exception
     */
    public JsonApiDocument readJsonApiDocument(JsonNode node) throws IOException {
        return mapper.treeToValue(node,  JsonApiDocument.class);
    }

    /**
     * Gets the Jackson ObjectMapper.
     *
     * @return the Jackson ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return mapper;
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
