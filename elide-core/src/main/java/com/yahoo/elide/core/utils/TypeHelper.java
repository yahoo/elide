/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.utils;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Dynamic;
import com.yahoo.elide.core.type.Type;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities for handling types and aliases.
 */
public class TypeHelper {
    private static final String UNDERSCORE = "_";
    private static final String PERIOD = ".";
    private static final Set<Class<?>> PRIMITIVE_NUMBER_TYPES = Sets
            .newHashSet(short.class, int.class, long.class, float.class, double.class);
    private static final Set<Class<?>> NUMBER_TYPES = Sets
            .newHashSet(short.class, int.class, long.class, float.class, double.class,
                    Short.class, Integer.class, Long.class, Float.class, Double.class);

    /**
     * Determine whether a type is primitive number type
     *
     * @param type type to check
     * @return True is the type is primitive number type
     */
    public static boolean isPrimitiveNumberType(Type<?> type) {
        if (type instanceof Dynamic) {
            return false;
        }

        return PRIMITIVE_NUMBER_TYPES.contains(((ClassType) type).getCls());
    }

    /**
     * Determine whether a type is number type
     *
     * @param type type to check
     * @return True is the type is number type
     */
    public static boolean isNumberType(Class<?> type) {
        return NUMBER_TYPES.contains(type);
    }

    /**
     * Extend an type alias to the final type of an extension path
     *
     * @param alias      type alias to be extended, e.g. <code>a_b</code>
     * @param extension  path extension from aliased type, e.g. <code>[b.c]/[c.d]</code>
     * @param dictionary The entity dictionary
     * @return extended type alias, e.g. <code>a_b_c</code>
     */
    public static String extendTypeAlias(String alias, Path extension, EntityDictionary dictionary) {
        String result = alias;
        List<Path.PathElement> elements = extension.getPathElements();

        for (int i = 0; i < elements.size() - 1; i++) {
            Path.PathElement next = elements.get(i);
            if (dictionary.isComplexAttribute(next.getType(), next.getFieldName())) {
                result = result + "." + next.getFieldName();
            } else {
                result = appendAlias(result, elements.get(i).getFieldName());
            }
        }

        return result;
    }

    /**
     * Generate alias for representing a relationship path which dose not include the last field name.
     * The path would start with the class alias of the first element, and then each field would append "_fieldName" to
     * the result.
     * The last field would not be included as that's not a part of the relationship path.
     *
     * @param path       path that represents a relationship chain
     * @param dictionary The entity dictionary
     * @return relationship path alias, i.e. <code>foo.bar.baz</code> would be <code>foo_bar</code>
     */
    public static String getPathAlias(Path path, EntityDictionary dictionary) {
        return extendTypeAlias(getTypeAlias(path.getPathElements().get(0).getType()), path, dictionary);
    }

    /**
     * Append a new field to a parent alias to get new alias.
     *
     * @param parentAlias parent path alias
     * @param fieldName   field name
     * @return alias for the field
     */
    public static String appendAlias(String parentAlias, String fieldName) {
        return isEmpty(parentAlias)
                ? fieldName
                : isEmpty(fieldName)
                ? parentAlias
                : parentAlias + UNDERSCORE + fieldName;
    }

    /**
     * Build an query friendly alias for a class.
     *
     * @param type The type to alias
     * @return type name alias that will likely not conflict with other types or with reserved keywords.
     */
    public static String getTypeAlias(Type<?> type) {
        return type.getCanonicalName().replace(PERIOD, UNDERSCORE);
    }

    /**
     * Get alias for the final field of a path.
     *
     * @param path       path to the field
     * @param fieldName  physical field name
     * @param dictionary the entity dictionary
     * @return combined alias
     */
    public static String getFieldAlias(Path path, String fieldName, EntityDictionary dictionary) {
        return getFieldAlias(getPathAlias(path, dictionary), fieldName);
    }

    /**
     * Get alias for the final field of a path.
     *
     * @param tableAlias alias for table that contains the field
     * @param fieldName  physical field name
     * @return combined alias
     */
    public static String getFieldAlias(String tableAlias, String fieldName) {
        return isEmpty(tableAlias) ? fieldName : tableAlias + PERIOD + fieldName;
    }

    /**
     * Converts a Set of classes to a set of types.
     * @param cls The set of classes.
     * @return A new set of types.
     */
    public static Set<Type<?>> getClassType(Set<Class<?>> cls) {
        return cls.stream()
                .map(ClassType::of)
                .collect(Collectors.toSet());
    }
}
