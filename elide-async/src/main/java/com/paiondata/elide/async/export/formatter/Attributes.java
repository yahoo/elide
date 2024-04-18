/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.request.Attribute;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Attributes methods.
 */
public class Attributes {
    /**
     * Gets the properties represented by the attributes. These can be nested.
     *
     * @param objectMapper the object mapper
     * @param attributes the attributes
     * @return the properties
     */
    public static Map<String, Object> getProperties(ObjectMapper objectMapper,
            Collection<? extends Attribute> attributes) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                String name = attribute.getName();
                Map<String, Object> properties = null;
                Class<?> attributeClass = attribute.getType().getUnderlyingClass().orElse(null);
                if (attributeClass != null) {
                    properties = getProperties(objectMapper, attributeClass);
                }
                result.put(name, properties);
            }
        }
        return result;
    }

    /**
     * Gets the properties of a class.
     *
     * @param objectMapper the object mapper
     * @param propertyClass the attributes
     * @return the properties
     */
    public static Map<String, Object> getProperties(ObjectMapper objectMapper, Class<?> propertyClass) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            for (Iterator<PropertyWriter> iterator = objectMapper.getSerializerProviderInstance()
                    .findValueSerializer(propertyClass).properties(); iterator.hasNext();) {
                PropertyWriter property = iterator.next();
                result.put(property.getName(), getProperties(objectMapper, property.getType().getRawClass()));
            }
        } catch (JsonMappingException e) {
            // Do nothing
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    /**
     * Gets the string representation of the attribute arguments.
     *
     * @param attribute the attribute
     * @return the string representation of the attribute arguments
     */
    public static String getArguments(Attribute attribute) {
        if (attribute.getArguments() == null || attribute.getArguments().size() == 0) {
            return "";
        }
        StringBuilder arguments = new StringBuilder();
        arguments.append("(");
        arguments.append(attribute.getArguments()
                .stream()
                .map((arg) -> arg.getName() + "=" + arg.getValue())
                .collect(Collectors.joining(" ")));
        arguments.append(")");
        return arguments.toString();
    }

    /**
     * Gets attributes of a resource.
     *
     * @param resource the resource
     * @param useAlias use the alias for the field name
     * @return the attributes
     */
    public static Map<String, Object> getAttributes(PersistentResource<?> resource, boolean useAlias) {
        final Map<String, Object> attributes = new LinkedHashMap<>();
        final Set<Attribute> attrFields = resource.getRequestScope().getEntityProjection().getAttributes();

        for (Attribute field : attrFields) {
            String fieldName = field.getName();
            if (useAlias) {
                String alias = field.getAlias();
                fieldName = StringUtils.isNotEmpty(alias) ? alias : field.getName();
            }
            attributes.put(fieldName, resource.getAttribute(field));
        }
        return attributes;
    }

    /**
     * Gets attributes of a resource.
     *
     * @param resource the resource
     * @return the attributes
     */
    public static Map<String, Object> getAttributes(PersistentResource<?> resource) {
        return getAttributes(resource, false);
    }


    /**
     * Gets the headers given the attributes.
     * <p>
     * Each individual header is a list to handle nested objects.
     *
     * @param objectMapper the object mapper
     * @param attributes the attributes
     * @return the headers which can be nested
     */
    public static List<List<String>> getHeaders(ObjectMapper objectMapper, Collection<? extends Attribute> attributes) {
        Map<String, Object> properties = Attributes.getProperties(objectMapper, attributes);
        return Attributes.getHeaders(properties);
    }

    /**
     * Gets the headers given the attribute properties.
     * <p>
     * Each individual header is a list to handle nested objects.
     *
     * @param properties the attribute properties
     * @return the headerswhich can be nested
     */
    public static List<List<String>> getHeaders(Map<String, Object> properties) {
        List<List<String>> headers = new ArrayList<>();
        for (Entry<String, Object> property : properties.entrySet()) {
            getHeaders(headers, property, null);
        }
        return headers;
    }

    /**
     * Gets the headers.
     *
     * @param result the final result to add to
     * @param property the property
     * @param prefix the current prefix
     */
    public static void getHeaders(List<List<String>> result, Entry<String, Object> property, List<String> prefix) {
        String header = property.getKey();
        if (property.getValue() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) property.getValue();
            for (Entry<String, Object> entry : values.entrySet()) {
                List<String> value = prefix != null ? new ArrayList<>(prefix) : new ArrayList<>();
                value.add(header);
                getHeaders(result, entry, value);
            }
        } else {
            List<String> value = prefix != null ? new ArrayList<>(prefix) : new ArrayList<>();
            value.add(header);
            result.add(value);
        }
    }
}
