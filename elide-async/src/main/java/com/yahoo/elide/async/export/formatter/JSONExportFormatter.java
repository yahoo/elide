/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.core.PersistentResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * JSON output format implementation.
 */
@Slf4j
public class JSONExportFormatter implements TableExportFormatter {
    private static final String COMMA = ",";
    private ObjectMapper mapper = new ObjectMapper();

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

        try {
            str.append(mapper.writeValueAsString(resource.getObject()));
        } catch (JsonProcessingException e) {
            log.error("Exception when converting to JSON {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        return str.toString();
    }
}
