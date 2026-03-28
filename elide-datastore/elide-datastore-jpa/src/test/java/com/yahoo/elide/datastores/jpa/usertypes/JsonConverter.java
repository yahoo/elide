/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa.usertypes;

import com.yahoo.elide.core.exceptions.InvalidValueException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * JsonType serializes an object to json string and vice versa.
 */
@Converter
public class JsonConverter<T> implements AttributeConverter<T, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Class<T> objectClass;

    public JsonConverter(Class<T> objectClass) {
        this.objectClass = objectClass;
    }

    @Override
    public String convertToDatabaseColumn(T value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new InvalidValueException("Unable to serialize", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String rawJson) {
        try {
            return MAPPER.readValue(rawJson, objectClass);
        } catch (JacksonException e) {
            throw new InvalidValueException("Unable to deserialize", e);
        }
    }
}
