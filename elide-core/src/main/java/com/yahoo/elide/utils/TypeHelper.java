/*
 * Copyright 2019, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Utilities for checking classes and primitive types.
 */
public class TypeHelper {
    private static final Set<Class<?>> PRIMITIVE_NUMBER_TYPES = Sets
            .newHashSet(short.class, int.class, long.class, float.class, double.class);

    /**
     * Determine whether a type is primitive number type
     *
     * @param type type to check
     * @return True is the type is primitive number type
     */
    public static boolean isPrimitiveNumberType(Class<?> type) {
        return PRIMITIVE_NUMBER_TYPES.contains(type);
    }
}
