/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.core.PersistentResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * {@link ResourceWriter} that writes in JSON format.
 */
@Slf4j
public class JsonResourceWriter extends ResourceWriterSupport {
    private static final String COMMA = ",";
    private final ObjectMapper objectMapper;
    private int recordCount = 0;

    public JsonResourceWriter(OutputStream outputStream, ObjectMapper objectMapper) {
        super(outputStream);
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(PersistentResource<?> resource) throws IOException {
        if (recordCount == 0) {
           preFormat(this.outputStream);
        }
        recordCount++;
        format(resource, this.outputStream);
    }

    @Override
    public void close() throws IOException {
        if (recordCount == 0) {
            preFormat(this.outputStream);
        }
        postFormat(outputStream);
    }

    public void format(PersistentResource<?> resource, OutputStream outputStream)
            throws IOException {
        if (resource == null) {
            return;
        }

        StringBuilder str = new StringBuilder();
        if (this.recordCount > 1) {
            // Add "," to separate individual json rows within the array
            str.append(COMMA);
        }

        str.append(resourceToJSON(objectMapper, resource));
        str.append('\n');
        write(str.toString());
    }

    public static String resourceToJSON(ObjectMapper mapper, PersistentResource<?> resource) {
        if (resource == null || resource.getObject() == null) {
            return null;
        }
        StringBuilder str = new StringBuilder();
        try {
            str.append(mapper.writeValueAsString(Attributes.getAttributes(resource, true)));
        } catch (JsonProcessingException e) {
            log.error("Exception when converting to JSON {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        return str.toString();
    }

    public void preFormat(OutputStream outputStream) throws IOException {
        outputStream.write("[\n".getBytes(StandardCharsets.UTF_8));
    }

    public void postFormat(OutputStream outputStream) throws IOException {
        outputStream.write("]\n".getBytes(StandardCharsets.UTF_8));
    }
}
