/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.utils.coerce.converters.FromMapConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;
import com.yahoo.elide.utils.coerce.converters.ToEnumConverter;
import com.yahoo.elide.utils.coerce.converters.ToUUIDConverter;

import com.google.common.collect.MapMaker;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Class for coercing a value to a target class.
 */
public class CoerceUtil {

    private static final ToEnumConverter TO_ENUM_CONVERTER = new ToEnumConverter();
    private static final ToUUIDConverter TO_UUID_CONVERTER = new ToUUIDConverter();
    private static final FromMapConverter FROM_MAP_CONVERTER = new FromMapConverter();
    private static final Map<Class<?>, Serde<?, ?>> SERDES = new HashMap<>();
    private static final BeanUtilsBean BEAN_UTILS_BEAN_INSTANCE = setup();
    private static final Set<ClassLoader> INITIALIZED_CLASSLOADERS =
            Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    /**
     * Convert value to target class.
     *
     * @param <T> type
     * @param value value to convert
     * @param cls   class to convert to
     * @return coerced value
     */
    public static <T> T coerce(Object value, Class<T> cls) {
        initializeCurrentClassLoaderIfNecessary();

        if (value == null || cls == null || cls.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        try {
            return (T) ConvertUtils.convert(value, cls);
        } catch (ConversionException | InvalidAttributeException | IllegalArgumentException e) {
            throw new InvalidValueException(value, e.getMessage());
        }
    }

    public static <S, T> void register(Class<T> targetType, Serde<S, T> serde) {
        initializeCurrentClassLoaderIfNecessary();

        SERDES.put(targetType, serde);
        ConvertUtils.register(new Converter() {

            @Override
            public <T> T convert(Class<T> aClass, Object o) {
                return (T) serde.deserialize((S) o);
            }

        }, targetType);
    }

    public static <S, T> Serde<S, T> lookup(Class<T> targetType) {
        return (Serde<S, T>) SERDES.getOrDefault(targetType, null);
    }

    /**
     * Perform CoerceUtil setup.
     */
    private static BeanUtilsBean setup() {
        return new BeanUtilsBean(new BidirectionalConvertUtilBean() {
            {
                // https://github.com/yahoo/elide/issues/260
                // enable throwing exceptions when conversion fails
                register(true, false, 0);
                register(TO_UUID_CONVERTER, UUID.class);
            }

            @Override
            /*
             * Overriding lookup to execute enum converter if target is enum
             * or map convert if source is map
             */
            public Converter lookup(Class<?> sourceType, Class<?> targetType) {
                if (targetType.isEnum()) {
                    return TO_ENUM_CONVERTER;
                } else if (Map.class.isAssignableFrom(sourceType)) {
                    return FROM_MAP_CONVERTER;
                } else {
                    return super.lookup(sourceType, targetType);
                }
            }
        });
    }

    /**
     * Initialize this classloader if necessary
     */
    private static void initializeCurrentClassLoaderIfNecessary() {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        if (INITIALIZED_CLASSLOADERS.contains(currentClassLoader)) {
            return;
        }
        BeanUtilsBean.setInstance(BEAN_UTILS_BEAN_INSTANCE);
        markClassLoaderAsInitialized(currentClassLoader);
    }

    /**
     * Mark the current class loader as initialized.
     * @param currentClassLoader current ClassLoader
     */
    private static void markClassLoaderAsInitialized(ClassLoader currentClassLoader) {
        INITIALIZED_CLASSLOADERS.add(currentClassLoader);
    }
}
