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
     * Extend an type alias to the final type of an extension path
     *
     * @param alias type alias to be extended, e.g. <code>a_b</code>
     * @param extension path extension from aliased type, e.g. <code>[b.c]/[c.d]</code>
     * @return extended type alias, e.g. <code>a_b_c</code>
     */
    public static String extendTypeAlias(String alias, Path extension) {
        List<Path.PathElement> elements = extension.getPathElements();

        for (int i = 0; i < elements.size() - 1; i++) {
            alias = appendAlias(alias, elements.get(i).getFieldName());
        }

        return alias;
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
        return extendTypeAlias(getTypeAlias(path.getPathElements().get(0).getType()), path);
    }

    /**
     * Append a new field to a parent alias to get new alias.
     *
     * @param parentAlias parent path alias
     * @param fieldName field name
     * @return alias for the field
     */
    public static String appendAlias(String parentAlias, String fieldName) {
        return nullOrEmpty(parentAlias)
                ? fieldName
                : nullOrEmpty(fieldName)
                        ? parentAlias
                        : parentAlias + UNDERSCORE + fieldName;
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

    /**
     * Get alias for the final field of a path.
     *
     * @param path path to the field
     * @param fieldName physical field name
     * @return combined alias
     */
    public static String getFieldAlias(Path path, String fieldName) {
        return getFieldAlias(getPathAlias(path), fieldName);
    }

    /**
     * Get alias for the final field of a path.
     *
     * @param tableAlias alias for table that contains the field
     * @param fieldName physical field name
     * @return combined alias
     */
    public static String getFieldAlias(String tableAlias, String fieldName) {
        return nullOrEmpty(tableAlias) ? fieldName : tableAlias + PERIOD + fieldName;
    }

    /**
     * Check whether an alias is null or empty string
     *
     * @param alias alias
     * @return True if is null or empty
     */
    private static boolean nullOrEmpty(String alias) {
        return alias == null || alias.equals("");
    }
}
