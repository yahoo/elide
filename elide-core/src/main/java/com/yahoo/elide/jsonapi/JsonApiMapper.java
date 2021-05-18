/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Patch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.util.List;

/**
 * Serializer/Deserializer for JSON API.
 */
public class JsonApiMapper {
    private final ObjectMapper mapper;

    /**
     * Instantiates a new Json Api Mapper.
     */
    public JsonApiMapper() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(JsonApiSerializer.getModule());
    }

    /**
     * Instantiates a new Json Api Mapper.
     *
     * @param mapper Custom object mapper to use internally for serializing/deserializing
     */
    public JsonApiMapper(ObjectMapper mapper) {
        this.mapper = mapper;
        mapper.registerModule(JsonApiSerializer.getModule());
    }

    /**
     * Write out JSON API Document as a string.
     *
     * @param jsonApiDocument the json api document
     * @return Document as string
     * @throws JsonProcessingException the json processing exception
     */
    public String writeJsonApiDocument(JsonApiDocument jsonApiDocument) throws JsonProcessingException {
        return mapper.writeValueAsString(jsonApiDocument);
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
     * @param node the node
     * @return the string
     * @throws JsonProcessingException the json processing exception
     */
    public String writeJsonApiDocument(JsonNode node) throws JsonProcessingException {
        return mapper.writeValueAsString(node);
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
     * Read json api patch ext value.
     *
     * @param value the value
     * @return the json api document
     * @throws JsonProcessingException the json processing exception
     */
    public JsonApiDocument readJsonApiPatchExtValue(JsonNode value) throws JsonProcessingException {
        JsonNode data = JsonNodeFactory.instance.objectNode().set("data", value);
        return mapper.treeToValue(data, JsonApiDocument.class);
    }

    /**
     * Read json api patch ext doc.
     *
     * @param doc the doc
     * @return the list
     * @throws IOException the iO exception
     */
    public List<Patch> readJsonApiPatchExtDoc(String doc) throws IOException {
        return mapper.readValue(doc, mapper.getTypeFactory().constructCollectionType(List.class, Patch.class));
    }

    /**
     * Gets object OBJECT_MAPPER.
     *
     * @return the object OBJECT_MAPPER
     */
    public ObjectMapper getObjectMapper() {
        return mapper;
    }
}
