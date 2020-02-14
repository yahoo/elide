/*
 * Copyright 2019, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.utils;

import com.yahoo.elide.core.Path;

import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Utilities for handling types and aliases.
 */
public class TypeHelper {
    private static final String UNDERSCORE = "_";
    private static final String PERIOD = ".";
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

    /**
     * Generate alias for representing a relationship path which dose not include the last field name.
     * The path would start with the class alias of the first element, and then each field would append "_fieldName" to
     * the result.
     * The last field would not be included as that's not a part of the relationship path.
     *
     * @param path path that represents a relationship chain
     * @return relationship path alias, i.e. <code>foo.bar.baz</code> would be <code>foo_bar</code>
     */
    public static String getPathAlias(Path path) {
        List<Path.PathElement> elements = path.getPathElements();
        String alias = getTypeAlias(elements.get(0).getType());

        for (int i = 0; i < elements.size() - 1; i++) {
            alias = appendAlias(alias, elements.get(i).getFieldName());
        }

        return alias;
    }

    /**
     * Append a new field to a parent alias to get new alias.
     *
     * @param parentAlias parent path alias
     * @param fieldName field name
     * @return alias for the field
     */
    public static String appendAlias(String parentAlias, String fieldName) {
        return parentAlias + UNDERSCORE + fieldName;
    }

    /**
     * Build an query friendly alias for a class.
     *
     * @param type The type to alias
     * @return type name alias that will likely not conflict with other types or with reserved keywords.
     */
    public static String getTypeAlias(Class<?> type) {
        return type.getCanonicalName().replace(PERIOD, UNDERSCORE);
    }
}
