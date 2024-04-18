/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.core.PersistentResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * {@link ResourceWriter} that writes in JSON format.
 */
@Slf4j
public class JsonResourceWriter extends ResourceWriterSupport {
    protected static final String COMMA = ",";
    protected final ObjectMapper objectMapper;
    protected int recordCount = 0;

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
        if (!closed) {
            if (recordCount == 0) {
                preFormat(this.outputStream);
            }
            postFormat(outputStream);
            super.close();
            closed = true;
        }
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

        str.append(convert(objectMapper, resource));
        str.append('\n');
        write(str.toString());
    }

    protected String convert(ObjectMapper mapper, PersistentResource<?> resource) {
        if (resource == null || resource.getObject() == null) {
            return null;
        }
        return convert(mapper, getAttributes(resource));
    }

    protected String convert(ObjectMapper mapper, Map<String, Object> attributes) {
        try {
            return mapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            log.error("Exception when converting to JSON {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets the attributes from a resource.
     *
     * @param resource the resource
     * @return the attributes
     */
    protected Map<String, Object> getAttributes(PersistentResource<?> resource) {
        return Attributes.getAttributes(resource, true);
    }

    public void preFormat(OutputStream outputStream) throws IOException {
        outputStream.write("[\n".getBytes(StandardCharsets.UTF_8));
    }

    public void postFormat(OutputStream outputStream) throws IOException {
        outputStream.write("]\n".getBytes(StandardCharsets.UTF_8));
    }
}
