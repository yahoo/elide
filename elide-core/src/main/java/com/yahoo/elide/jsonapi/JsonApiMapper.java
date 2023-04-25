/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperationsMapper;
import com.yahoo.elide.jsonapi.extensions.JsonApiPatchMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Serializer/Deserializer for JSON API.
 */
public class JsonApiMapper {
    private final ObjectMapper mapper;
    private final JsonApiPatchMapper jsonPatchMapper;
    private final JsonApiAtomicOperationsMapper atomicOperationsMapper;

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
        this.mapper = mapper;
        this.mapper.registerModule(JsonApiSerializer.getModule());
        this.jsonPatchMapper = new JsonApiPatchMapper(this.mapper);
        this.atomicOperationsMapper = new JsonApiAtomicOperationsMapper(this.mapper);
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
    public JsonApiPatchMapper forJsonPatch() {
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
