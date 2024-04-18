/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils.coerce.converters;

import com.paiondata.elide.core.exceptions.InvalidAttributeException;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.ClassUtils;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;


/**
 * Convert epoch(in string or long) to Date.
 * @param <T> Date Type
 */
public class EpochToDateConverter<T extends Date> implements Converter, Serde<Object, T> {

    private Class<T> targetType;

    public EpochToDateConverter(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public <T> T convert(Class<T> cls, Object value) {
        try {
            if (ClassUtils.isAssignable(value.getClass(), String.class)) {
                return stringToDate(cls, (String) value);
            }
            if (ClassUtils.isAssignable(value.getClass(), Number.class, true)) {
                return numberToDate(cls, (Number) value);
            }
            throw new UnsupportedOperationException(value.getClass().getSimpleName() + " is not a valid epoch");
        } catch (IndexOutOfBoundsException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new InvalidAttributeException("Unknown " + cls.getSimpleName() + " value " + value, e);
        }
    }

    @Override
    public T deserialize(Object val) {
        return convert(targetType, val);
    }

    @Override
    public Long serialize(T val) {
        return val.getTime();
    }

    private static <T> T numberToDate(Class<T> cls, Number epoch) {
        if (ClassUtils.isAssignable(cls, java.sql.Date.class)) {
            return cls.cast(new java.sql.Date(epoch.longValue()));
        }
        if (ClassUtils.isAssignable(cls, Timestamp.class)) {
            return cls.cast(new Timestamp(epoch.longValue()));
        }
        if (ClassUtils.isAssignable(cls, Time.class)) {
            return cls.cast(new Time(epoch.longValue()));
        }
        if (ClassUtils.isAssignable(cls, Date.class)) {
            return cls.cast(new Date(epoch.longValue()));
        }
        throw new UnsupportedOperationException("Cannot convert to " + cls.getSimpleName());
    }

    private static <T> T stringToDate(Class<T> cls, String epoch) {
        return numberToDate(cls, Long.parseLong(epoch));
    }
}
