/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils.coerce.converters;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.ClassUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A class that knows how to convert thing to other (different) things.
 */
public class ElideConverter extends ConvertUtilsBean {
    private static final ToEnumConverter TO_ENUM_CONVERTER = new ToEnumConverter();
    private static final FromMapConverter FROM_MAP_CONVERTER = new FromMapConverter();
    private static final EpochToDateConverter EPOCH_TO_DATE_CONVERTER = new EpochToDateConverter();

    private LinkedHashMap<BiFunction<Class<?>, Class<?>, Boolean>, Converter> converters;

    /**
     * Create a new ElideConverter with a set of converters.
     *
     * @param converters extra converters to consider before the default converters fire
     */
    public ElideConverter(LinkedHashMap<BiFunction<Class<?>, Class<?>, Boolean>, Converter> converters) {
        // yahoo/elide#260 - enable throwing exceptions when conversion fails
        register(true, false, 0);
        this.converters = converters;
    }

    /**
     * Create a new ElideConverter with the default set of converters.
     */
    public ElideConverter() {
        this(defaultConverters());
    }

    @Override
    /*
     * Overriding lookup to execute enum converter if target is enum
     * or map convert if source is map
     */
    public Converter lookup(Class<?> sourceType, Class<?> targetType) {
        for (Map.Entry<BiFunction<Class<?>, Class<?>, Boolean>, Converter> entry : converters.entrySet()) {
            if (entry.getKey().apply(sourceType, targetType)) {
                return entry.getValue();
            }
        }
        return super.lookup(sourceType, targetType);
    }

    public static LinkedHashMap<BiFunction<Class<?>, Class<?>, Boolean>, Converter> defaultConverters() {
        LinkedHashMap<BiFunction<Class<?>, Class<?>, Boolean>, Converter> converters = new LinkedHashMap<>();
        converters.put((source, target) -> target.isEnum(), TO_ENUM_CONVERTER);
        converters.put((source, target) -> Map.class.isAssignableFrom(source), FROM_MAP_CONVERTER);
        converters.put(
                (source, target) -> (String.class.isAssignableFrom(source) || Number.class.isAssignableFrom(source))
                        && ClassUtils.isAssignable(target, Date.class),
                EPOCH_TO_DATE_CONVERTER);
        return converters;
    }
}
