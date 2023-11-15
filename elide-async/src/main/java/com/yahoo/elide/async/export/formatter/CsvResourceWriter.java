/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link ResourceWriter} that writes in CSV format.
 */
@Slf4j
public class CsvResourceWriter extends ResourceWriterSupport {
    private static final String LIST_SEPARATOR = ";";
    private static final String HEADER_SEPARATOR = "_";
    private static final String ITEM_SEPARATOR = ",";
    private static final String LINE_SEPARATOR = "\r\n"; // CRLF rfc4180

    private boolean writeHeader = true;
    private List<List<String>> headers;
    private ObjectMapper objectMapper;
    private EntityProjection entityProjection;
    private int recordCount = 0;

    public CsvResourceWriter(OutputStream outputStream, ObjectMapper objectMapper, boolean writeHeader,
            EntityProjection entityProjection) {
        super(outputStream);
        this.writeHeader = writeHeader;
        this.objectMapper = objectMapper;
        this.entityProjection = entityProjection;
        this.headers = entityProjection != null ? Attributes.getHeaders(objectMapper, entityProjection.getAttributes())
                : Collections.emptyList();
    }

    @Override
    public void write(PersistentResource<?> resource) throws IOException {
        if (recordCount == 0) {
            preFormat(this.outputStream);
        }
        recordCount++;
        format(resource);
    }

    @Override
    public void close() throws IOException {
        if (recordCount == 0) {
            preFormat(this.outputStream);
        }
        super.close();
    }

    public void format(PersistentResource<?> resource) throws IOException {
        if (resource == null) {
            return;
        }

        @SuppressWarnings("rawtypes")
        Map values = objectMapper.convertValue(Attributes.getAttributes(resource), Map.class);
        List<Object> result = headers.stream().map(header -> {
            return getValue(header, values);
        }).toList();

        String line = result.stream().map(this::toString).map(this::quote).collect(Collectors.joining(ITEM_SEPARATOR));
        write(line + LINE_SEPARATOR);
    }

    public void preFormat(OutputStream outputStream) throws IOException {
        if (this.entityProjection == null || !writeHeader) {
            return;
        }
        write(generateCSVHeader(this.entityProjection));
    }

    /**
     * Generate CSV Header when Observable is Empty.
     *
     * @param projection EntityProjection object.
     * @return returns Header string which is in CSV format.
     */
    private String generateCSVHeader(EntityProjection projection) {
        if (projection.getAttributes() == null) {
            return "";
        }

        Map<String, Attribute> attributes = projection.getAttributes().stream()
                .collect(Collectors.toMap(Attribute::getName, Function.identity()));
        String result = headers.stream().map(header -> {
           StringBuilder headerBuilder = new StringBuilder();
           Attribute attribute = attributes.get(header.get(0));
           for (int x = 0; x < header.size(); x++) {
               String item = header.get(x);
               if (x == 0 && !StringUtils.isEmpty(attribute.getAlias())) {
                   item = attribute.getAlias();
               }
               if (x != 0) {
                  headerBuilder.append(HEADER_SEPARATOR);
               }
               headerBuilder.append(item);
           }
           String arguments = Attributes.getArguments(attribute);
           if (!"".equals(arguments)) {
               headerBuilder.append(arguments);
           }
           return quote(headerBuilder.toString());
        }).collect(Collectors.joining(ITEM_SEPARATOR));

        if (!"".equals(result)) {
            return result + LINE_SEPARATOR;
        }
        return result;
    }

    private String toString(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof Collection collection) {
            return collection.stream().map(this::toString).collect(Collectors.joining(LIST_SEPARATOR)).toString();
        }
        return CoerceUtil.coerce(object, String.class);
    }

    private Object getValue(List<String> header, Map values) {
        Object value = null;
        for (int x = 0; x < header.size(); x++) {
            String item = header.get(x);
            if (x == 0) {
                value = values.get(item);
            } else {
                if (value instanceof Map m) {
                    value = m.get(item);
                }
            }
        }
        return value;
     }

     private String quote(String toQuote) {
        String escaped = toQuote.replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }
}
