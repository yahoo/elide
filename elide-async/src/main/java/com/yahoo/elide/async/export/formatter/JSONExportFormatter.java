/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * JSON output format implementation.
 */
@Slf4j
public class JSONExportFormatter implements TableExportFormatter {
    private static final String COMMA = ",";
    private JsonApiMapper jsonMapper;

    public JSONExportFormatter(Elide elide) {
        this.jsonMapper = elide.getMapper();
    }

    @Override
    public String format(PersistentResource resource, Integer recordNumber) {
        if (resource == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();
        if (recordNumber > 1) {
            // Add "," to separate individual json rows within the array
            str.append(COMMA);
        }

        str.append(resourceToJSON(jsonMapper, resource));
        return str.toString();
    }

    public static String resourceToJSON(JsonApiMapper jsonMapper, PersistentResource resource) {
        if (resource == null || resource.getObject() == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();
        try {
            JsonApiDocument jsonApiDocument = new JsonApiDocument();
            //TODO Make this a document processor
            Data<Resource> data = resource == null ? null
                    : new Data<>(resource.toResource(getRelationships(resource), getAttributes(resource)));
            jsonApiDocument.setData(data);

            // jsonApiDcocument translates to
            // {"data": {"type": <name>, "id": <id>, "attributes": {"attr1": <value>, <attr2>: value ... }}}
            // So extracting only attributes from it.
            JsonNode node = jsonMapper.toJsonObject(jsonApiDocument).get("data").get("attributes");
            str.append(jsonMapper.writeJsonApiDocument(node));
        } catch (JsonProcessingException e) {
            log.error("Exception when converting to JSON {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        return str.toString();
    }

    protected static Map<String, Object> getAttributes(PersistentResource resource) {
        final Map<String, Object> attributes = new LinkedHashMap<>();
        final Set<Attribute> attrFields = resource.getRequestScope().getEntityProjection().getAttributes();

        for (Attribute field : attrFields) {
            attributes.put(field.getName(), resource.getAttribute(field));
        }
        return attributes;
    }

    protected static Map<String, Object> getRelationships(PersistentResource resource) {
        return Collections.emptyMap();
    }

    @Override
    public String preFormat(EntityProjection projection, TableExport query) {
        return "[";
    }

    @Override
    public String postFormat(EntityProjection projection, TableExport query) {
        return "]";
    }
}
