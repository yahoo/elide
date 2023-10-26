/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.jsonapi.models.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * JSON output format implementation.
 */
@Slf4j
public class JsonExportFormatter implements TableExportFormatter {
    private static final String COMMA = ",";
    private ObjectMapper mapper;

    public JsonExportFormatter(Elide elide) {
        this.mapper = elide.getObjectMapper();
    }

    @Override
    public void format(TableExportFormatterContext context, PersistentResource<?> resource, OutputStream outputStream)
            throws IOException {
        if (resource == null) {
            return;
        }

        StringBuilder str = new StringBuilder();
        if (context.getRecordNumber() > 1) {
            // Add "," to separate individual json rows within the array
            str.append(COMMA);
        }

        str.append(resourceToJSON(mapper, resource));
        str.append('\n');
        outputStream.write(str.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String resourceToJSON(ObjectMapper mapper, PersistentResource<?> resource) {
        if (resource == null || resource.getObject() == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();
        try {
            Resource jsonResource = resource.toResource(getRelationships(resource), getAttributes(resource));

            str.append(mapper.writeValueAsString(jsonResource.getAttributes()));
        } catch (JsonProcessingException e) {
            log.error("Exception when converting to JSON {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        return str.toString();
    }

    private static Map<String, Object> getAttributes(PersistentResource<?> resource) {
        final Map<String, Object> attributes = new LinkedHashMap<>();
        final Set<Attribute> attrFields = resource.getRequestScope().getEntityProjection().getAttributes();

        for (Attribute field : attrFields) {
            String alias = field.getAlias();
            String fieldName = StringUtils.isNotEmpty(alias) ? alias : field.getName();
            attributes.put(fieldName, resource.getAttribute(field));
        }
        return attributes;
    }

    private static <K, V> Map<K, V> getRelationships(PersistentResource<?> resource) {
        return Collections.emptyMap();
    }

    @Override
    public void preFormat(TableExportFormatterContext context, OutputStream outputStream) throws IOException {
        outputStream.write("[\n".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void postFormat(TableExportFormatterContext context, OutputStream outputStream) throws IOException {
        outputStream.write("]\n".getBytes(StandardCharsets.UTF_8));
    }
}
