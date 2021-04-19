/*
 * Copyright 2019, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.utils;

import static com.yahoo.elide.core.filter.dialect.RSQLFilterDialect.FILTER_ARGUMENTS_PATTERN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Dynamic;
import com.yahoo.elide.core.type.Type;

import com.google.common.collect.Sets;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
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
     * @param alias type alias to be extended, e.g. <code>a_b</code>
     * @param extension path extension from aliased type, e.g. <code>[b.c]/[c.d]</code>
     * @return extended type alias, e.g. <code>a_b_c</code>
     */
    public static String extendTypeAlias(String alias, Path extension) {
        String result = alias;
        List<Path.PathElement> elements = extension.getPathElements();

        for (int i = 0; i < elements.size() - 1; i++) {
            result = appendAlias(result, elements.get(i).getFieldName());
        }

        return result;
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
        return isEmpty(tableAlias) ? fieldName : tableAlias + PERIOD + fieldName;
    }

    /**
     * Construction helper.
     * @param cls
     * @return wrapped Type
     * @deprecated Use {@link ClassType#of(Class)}
     */
    @Deprecated
    public static Type<?> getClassType(Class<?> cls) {
        return ClassType.of(cls);
    }

    public static Set<Type<?>> getClassType(Set<Class<?>> cls) {
        return cls.stream()
                        .map(ClassType::of)
                        .collect(Collectors.toSet());
    }

    /**
     * Parses input string and return arguments as map.
     *
     * @param argsString String to parse for arguments.
     * @return A Map of argument's name and value.
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> parseArguments(String argsString) throws UnsupportedEncodingException {
        Map<String, String> arguments = new HashMap<>();

        if (!isEmpty(argsString)) {

            Matcher matcher = FILTER_ARGUMENTS_PATTERN.matcher(argsString);
            while (matcher.find()) {
                arguments.put(matcher.group(1),
                              URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8.name()));
            }
        }
        return arguments;
    }
}
