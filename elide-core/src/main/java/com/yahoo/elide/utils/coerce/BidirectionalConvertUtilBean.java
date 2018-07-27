/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.utils.coerce;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows registration based on source and target type (rather than just the target type).
 */
public class BidirectionalConvertUtilBean extends ConvertUtilsBean {
    protected Map<Pair<Class<?>, Class<?>>, Converter> bidirectionalConverters = new HashMap<>();

    public void register(Class<?> sourceType, Class<?> targetType, Converter converter) {
        Pair<Class<?>, Class<?>> key = Pair.of(sourceType, targetType);

        bidirectionalConverters.put(key, converter);
    }

    @Override
    public Converter lookup(Class<?> sourceType, Class<?> targetType) {
        Pair<Class<?>, Class<?>> key = Pair.of(sourceType, targetType);

        if (bidirectionalConverters.containsKey(key)) {
            return bidirectionalConverters.get(key);
        }

        return super.lookup(sourceType, targetType);
    }
}
