/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;

import java.util.Optional;

public class GraphQLNameUtils {
    private static final String MAP_SUFFIX = "Map";
    private static final String INPUT_SUFFIX = "Input";
    private static final String NODE_SUFFIX = "Node";
    private static final String EDGES_SUFFIX = "Edges";

    private final EntityDictionary dictionary;

    public GraphQLNameUtils(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public String toOutputTypeName(Class<?> clazz) {
        return Optional.ofNullable(dictionary.getJsonAliasFor(clazz))
                       .orElse(clazz.getSimpleName());
    }

    public String toInputTypeName(Class<?> clazz) {
        return toOutputTypeName(clazz) + INPUT_SUFFIX;
    }

    public String toMapEntryOutputName(Class<?> keyClazz, Class<?> valueClazz) {
        return toOutputTypeName(keyClazz) + toOutputTypeName(valueClazz) + MAP_SUFFIX;
    }

    public String toMapEntryInputName(Class<?> keyClazz, Class<?> valueClazz) {
        return toOutputTypeName(keyClazz) + toOutputTypeName(valueClazz) + MAP_SUFFIX + INPUT_SUFFIX;
    }

    public String toEdgesName(Class<?> clazz) {
        return toOutputTypeName(clazz) + EDGES_SUFFIX;
    }

    public String toNodeName(Class<?> clazz) {
        return toOutputTypeName(clazz) + NODE_SUFFIX;
    }
}
