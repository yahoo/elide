/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.beanutils.Converter;

/**
 * Uses Jackson to Convert from Map to target object.
 */
public class FromMapConverter implements Converter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Convert value from map to target object.
     *
     * @param cls class to convert to
     * @param value value to convert
     * @param <T> object type
     * @return converted object
     */
    @Override
    public <T> T convert(Class<T> cls, Object value) {
        return MAPPER.convertValue(value, cls);
    }
}
