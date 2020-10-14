/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import org.apache.commons.beanutils.Converter;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Converter from java.sql.Timestamp to java.sql.Date.
 */
public class SQLDateConverter implements Converter {
    /**
     * Convert value to UUID.
     *
     * @param cls class to convert to
     * @param value value to convert
     * @param <T> object type
     * @return converted object
     */
    @Override
    public <T> T convert(Class<T> cls, Object value) {
        if (cls.isAssignableFrom(Date.class) && value.getClass().isAssignableFrom(Timestamp.class)) {
            long epoch = ((Timestamp) value).getTime();
            return (T) new Date(epoch);
        }
        throw new UnsupportedOperationException("Cannot convert to " + cls.getSimpleName());
    }
}
