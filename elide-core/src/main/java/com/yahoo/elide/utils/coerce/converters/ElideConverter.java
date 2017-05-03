/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils.coerce.converters;

import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.ClassUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A class that knows how to convert thing to other (different) things.
 */
public class ElideConverter extends ConvertUtilsBean {
    private static final ToEnumConverter TO_ENUM_CONVERTER = new ToEnumConverter();
    private static final FromMapConverter FROM_MAP_CONVERTER = new FromMapConverter();
    private static final EpochToDateConverter EPOCH_TO_DATE_CONVERTER = new EpochToDateConverter();

    public static final BiFunction<Class<?>, Class<?>, Boolean> SOURCE_IS_MAP = (source, target) ->
            Map.class.isAssignableFrom(source);
    public static final BiFunction<Class<?>, Class<?>, Boolean> TARGET_IS_ENUM = (source, target) ->
            target.isEnum();
    public static final BiFunction<Class<?>, Class<?>, Boolean> STR_NUM_TO_DATE = (source, target) ->
            (String.class.isAssignableFrom(source) || Number.class.isAssignableFrom(source))
                    && ClassUtils.isAssignable(target, Date.class);
    public static final int HIGH_PRIORITY = 10;
    public static final int MEDIUM_PRIORITY = 20;
    public static final int LOW_PRIORITY = 30;

    private static SortedSetMultimap<Integer, TypeCoercer> CONVERTERS = Multimaps.synchronizedSortedSetMultimap(
            TreeMultimap.<Integer, TypeCoercer>create());

    /**
     * Create a new ElideConverter with a set of converters.
     *
     * @param converters extra converters to consider before the default converters fire
     */
    public ElideConverter(Map<Integer, TypeCoercer> converters) {
        register(true, false, 0); // #260 - throw exceptions when conversion fails
        converters.forEach((key, value) -> CONVERTERS.put(key, value));
    }

    /**
     * Create a new ElideConverter with the default set of converters.
     */
    public ElideConverter() {
        this(defaultConverters());
    }

    /*
     * Overriding lookup to execute enum converter if target is enum
     * or map convert if source is map
     */
    @Override
    public Converter lookup(Class<?> sourceType, Class<?> targetType) {
        return CONVERTERS.values().stream()
                .filter(tc ->  tc.discriminator.apply(sourceType, targetType))
                .map(TypeCoercer::getConverter)
                .findFirst()
                .orElse(super.lookup(sourceType, targetType));
    }

    public static Map<Integer, TypeCoercer> defaultConverters() {
        Map<Integer, TypeCoercer> converters = new HashMap<>();
        converters.put(HIGH_PRIORITY, new TypeCoercer(TARGET_IS_ENUM, TO_ENUM_CONVERTER));
        converters.put(MEDIUM_PRIORITY, new TypeCoercer(SOURCE_IS_MAP, FROM_MAP_CONVERTER));
        converters.put(LOW_PRIORITY, new TypeCoercer(STR_NUM_TO_DATE, EPOCH_TO_DATE_CONVERTER));
        return converters;
    }

    /**
     * Value type for registering converter with ElideConverter.
     */
    @RequiredArgsConstructor
    public static class TypeCoercer implements Comparable<TypeCoercer> {
        public final BiFunction<Class<?>, Class<?>, Boolean> discriminator;
        @Getter public final Converter converter;

        @Override
        public int compareTo(TypeCoercer o) {
            if (this == o) {
                return 0;
            }
            return Integer.compare(hashCode(), o.hashCode());
        }
    }
}
