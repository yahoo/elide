/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

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
public class CsvResourceWriter extends ResourceWriterSupport {
    /**
     * Used to delimit value that contains a list of values.
     */
    private static final String DEFAULT_LIST_SEPARATOR = ";";
    /**
     * Used to delimit a header with a nested object.
     */
    private static final String DEFAULT_HEADER_SEPARATOR = "_";
    /**
     * Used to delimit the items.
     */
    private static final String DEFAULT_ITEM_SEPARATOR = ",";
    /**
     * Used to delimit lines.
     */
    private static final String DEFAULT_LINE_SEPARATOR = "\r\n"; // CRLF RFC4180

    protected final boolean writeHeader;
    protected final ObjectMapper objectMapper;
    protected final EntityProjection entityProjection;

    /**
     * Each individual header is a list to handle nested objects.
     */
    protected List<List<String>> headers;

    protected String listSeparator = DEFAULT_LIST_SEPARATOR;
    protected String headerSeparator = DEFAULT_HEADER_SEPARATOR;
    protected String itemSeparator = DEFAULT_ITEM_SEPARATOR;
    protected String lineSeparator = DEFAULT_LINE_SEPARATOR;
    protected int recordCount = 0;

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
        if (!closed) {
            if (recordCount == 0) {
                preFormat(this.outputStream);
            }
            super.close();
            closed = true;
        }
    }

    public void format(PersistentResource<?> resource) throws IOException {
        if (resource == null) {
            return;
        }

        Map<String, Object> values = getAttributes(resource);
        List<Object> result = headers.stream().map(header -> {
            return getValue(header, values);
        }).toList();

        String line = result.stream()
                .map(this::toString)
                .map(this::quote)
                .collect(Collectors.joining(itemSeparator));
        write(line + lineSeparator);
    }

    /**
     * Gets the attributes from a resource.
     *
     * @param resource the resource
     * @return the attributes
     */
    protected Map<String, Object> getAttributes(PersistentResource<?> resource) {
        // The object mapper will convert the map with nested objects to maps with string values
        return objectMapper.convertValue(Attributes.getAttributes(resource),
                new TypeReference<Map<String, Object>>() {
                });
    }

    public void preFormat(OutputStream outputStream) throws IOException {
        if (this.entityProjection == null || !writeHeader) {
            return;
        }
        write(generateHeader(this.entityProjection));
    }

    /**
     * Generate CSV Header when Observable is Empty.
     *
     * @param projection EntityProjection object.
     * @return returns Header string which is in CSV format.
     */
    protected String generateHeader(EntityProjection projection) {
        if (projection.getAttributes() == null) {
            return "";
        }

        Map<String, Attribute> attributes = projection.getAttributes().stream()
                .collect(Collectors.toMap(Attribute::getName, Function.identity()));
        String result = headers.stream().map(header -> {
           String headerValue = getHeader(header, attributes);
           return quote(headerValue);
        }).collect(Collectors.joining(itemSeparator));

        if (!"".equals(result)) {
            return result + lineSeparator;
        }
        return result;
    }

    /**
     * Gets the header value.
     *
     * @param header the header
     * @param attributes the attributes
     * @return the header value
     */
    protected String getHeader(List<String> header, Map<String, Attribute> attributes) {
        StringBuilder headerBuilder = new StringBuilder();
        Attribute attribute = attributes.get(header.get(0));
        for (int x = 0; x < header.size(); x++) {
            String item = header.get(x);
            if (x == 0 && !StringUtils.isEmpty(attribute.getAlias())) {
                item = attribute.getAlias();
            }
            if (x != 0) {
                headerBuilder.append(headerSeparator);
            }
            headerBuilder.append(item);
        }
        String arguments = Attributes.getArguments(attribute);
        if (!"".equals(arguments)) {
            headerBuilder.append(arguments);
        }
        return headerBuilder.toString();
    }

    protected String toString(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof Collection<?> collection) {
            return convertCollection(collection);
        }
        return convert(object, String.class);
    }

    protected <T> T convert(Object value, Class<T> clazz) {
        return CoerceUtil.coerce(value, clazz);
    }

    /**
     * Converts a collection.
     *
     * @param collection the collection
     * @return the value
     */
    protected String convertCollection(Collection<?> collection) {
        return collection.stream().map(this::toString).collect(Collectors.joining(listSeparator));
    }

    protected Object getValue(List<String> header, Map<String, Object> values) {
        Object value = null;
        for (int x = 0; x < header.size(); x++) {
            String item = header.get(x);
            if (x == 0) {
                value = values.get(item);
            } else if (value instanceof Map map) {
                value = map.get(item);
            }
        }
        return value;
     }

    /**
     * Quote and escape the value to quote.
     * <p>
     * If double-quotes are used to enclose fields, then a double-quote appearing
     * inside a field must be escaped by preceding it with another double quote.
     *
     * @param toQuote the value to quote
     * @return the quoted value
     */
    protected String quote(String toQuote) {
        String escaped = toQuote.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
