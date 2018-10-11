/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce.converters;

import com.yahoo.elide.core.exceptions.InvalidAttributeException;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.ClassUtils;

/**
 * Converter to Enum.
 */
public class ToEnumConverter implements Converter {
    /**
     * Convert value to enum.
     *
     * @param cls enum to convert to
     * @param value value to convert
     * @param <T> enum type
     * @return enum
     */
    @Override
    public <T> T convert(Class<T> cls, Object value) {

        try {
            if (ClassUtils.isAssignable(value.getClass(), String.class)) {
                return stringToEnum(cls, (String) value);
            } else if (ClassUtils.isAssignable(value.getClass(), Integer.class, true)) {
                return intToEnum(cls, (Integer) value);
            } else {
               throw new UnsupportedOperationException(value.getClass().getSimpleName() + " to Enum no supported");
            }
        } catch (IndexOutOfBoundsException | ReflectiveOperationException
                | UnsupportedOperationException | IllegalArgumentException e) {
            throw new InvalidAttributeException("Unknown " + cls.getSimpleName() + " value " + value, e);
        }
    }

    /**
     * Convert digit to enum.
     *
     * @param cls enum to convert to
     * @param value value to convert
     * @param <T> enum type
     * @return enum
     * @throws ReflectiveOperationException reflection exception
     */
    private static <T> T intToEnum(Class<?> cls, Integer value) throws ReflectiveOperationException {
        Object[] values = (Object[]) cls.getMethod("values").invoke(null, (Object[]) null);
        return (T) values[value];
    }

    /**
     * Convert string to enum.
     *
     * @param cls enum to convert to
     * @param value value to convert
     * @param <T> enum type
     * @return enum
     */
    private static <T> T stringToEnum(Class<?> cls, String value) {
        return (T) Enum.valueOf((Class<Enum>) cls, value);
    }
}
