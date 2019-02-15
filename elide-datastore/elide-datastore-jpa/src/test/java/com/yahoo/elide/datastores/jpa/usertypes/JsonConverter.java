/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa.usertypes;

import com.yahoo.elide.core.exceptions.InvalidValueException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * JsonType serializes an object to json string and vice versa.
 */
@Converter
public class JsonConverter<T> implements AttributeConverter<T, String> {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final Class<T> objectClass;

    public JsonConverter(Class<T> objectClass) {
        this.objectClass = objectClass;
    }

    @Override
    public String convertToDatabaseColumn(T value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new InvalidValueException("Unable to serialize", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String rawJson) {
        try {
            return MAPPER.readValue(rawJson, objectClass);
        } catch (IOException e) {
            throw new InvalidValueException("Unable to deserialize", e);
        }
    }
}
