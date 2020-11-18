/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import org.apache.commons.lang3.StringUtils;

public class GraphQLNameUtils {
    private static final String MAP_SUFFIX = "Map";
    private static final String INPUT_SUFFIX = "Input";
    private static final String CONNECTION_SUFFIX = "Connection";
    private static final String EDGE_SUFFIX = "Edge";

    private final EntityDictionary dictionary;

    public GraphQLNameUtils(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public String toOutputTypeName(Class<?> clazz) {
        if (dictionary.hasBinding(clazz)) {
            return StringUtils.capitalize(dictionary.getJsonAliasFor(clazz));
        }
        return clazz.getSimpleName();
    }

    public String toInputTypeName(Class<?> clazz) {
        return toOutputTypeName(clazz) + INPUT_SUFFIX;
    }

    public String toMapEntryOutputName(Class<?> keyClazz, Class<?> valueClazz) {
        return toOutputTypeName(keyClazz) + toOutputTypeName(valueClazz) + MAP_SUFFIX;
    }

    public String toMapEntryInputName(Class<?> keyClazz, Class<?> valueClazz) {
        return toMapEntryOutputName(keyClazz, valueClazz) + INPUT_SUFFIX;
    }

    public String toEdgesName(Class<?> clazz) {
        return toOutputTypeName(clazz) + EDGE_SUFFIX;
    }

    public String toNodeName(Class<?> clazz) {
        return toOutputTypeName(clazz);
    }

    public String toConnectionName(Class<?> clazz) {
        return toOutputTypeName(clazz) + CONNECTION_SUFFIX;
    }

    public String toNonElideOutputTypeName(Class<?> clazz) {
        return StringUtils.uncapitalize(toOutputTypeName(clazz));
    }

    public String toNonElideInputTypeName(Class<?> clazz) {
        return StringUtils.uncapitalize(toInputTypeName(clazz));
    }
}
