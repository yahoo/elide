/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Patch;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.Serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.util.Date;
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

        mapper.registerModule(
                new SimpleModule("isoDate", Version.unknownVersion())
                        .addSerializer(Date.class, new JsonSerializer<Date>() {
                            @Override
                            public void serialize(Date date,
                                                  JsonGenerator jsonGenerator,
                                                  SerializerProvider serializerProvider)
                                    throws IOException, JsonProcessingException {
                                Serde<?, Date> serde = CoerceUtil.lookup(Date.class);

                                jsonGenerator.writeObject(serde.serialize(date));
                            }
                        }
                )
        );

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
     * Instantiates a new JSON API OBJECT_MAPPER.
     *
     * @param dictionary Not Used
     */
    @Deprecated
    public JsonApiMapper(EntityDictionary dictionary) {
        this();
    }

    /**
     * Instantiates a new Json Api Mapper.
     *
     * @param dictionary the dictionary
     * @param mapper Custom object mapper to use internally for serializing/deserializing
     */
    @Deprecated
    public JsonApiMapper(EntityDictionary dictionary, ObjectMapper mapper) {
        this(mapper);
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
