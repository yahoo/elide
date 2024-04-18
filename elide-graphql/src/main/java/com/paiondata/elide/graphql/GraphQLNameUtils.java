/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.graphql;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.Type;
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

    public String toOutputTypeName(Type<?> clazz) {
        String typeName;
        if (dictionary.hasBinding(clazz)) {
            typeName = StringUtils.capitalize(dictionary.getJsonAliasFor(clazz));
        } else {
            typeName = clazz.getSimpleName();
        }

        //Namespace internal types so they don't conflict with client models.
        if (clazz.getPackage().getName().startsWith("com.paiondata.elide")) {
            typeName = "Elide" + typeName;
        }

        return typeName;
    }

    public String toInputTypeName(Type<?> clazz) {
        return toOutputTypeName(clazz) + INPUT_SUFFIX;
    }

    public String toMapEntryOutputName(Type<?> keyClazz, Type<?> valueClazz) {
        return toOutputTypeName(keyClazz) + toOutputTypeName(valueClazz) + MAP_SUFFIX;
    }

    public String toMapEntryInputName(Type<?> keyClazz, Type<?> valueClazz) {
        return toMapEntryOutputName(keyClazz, valueClazz) + INPUT_SUFFIX;
    }

    public String toEdgesName(Type<?> clazz) {
        return toOutputTypeName(clazz) + EDGE_SUFFIX;
    }

    public String toNodeName(Type<?> clazz) {
        return toOutputTypeName(clazz);
    }

    public String toConnectionName(Type<?> clazz) {
        return toOutputTypeName(clazz) + CONNECTION_SUFFIX;
    }

    public String toNonElideOutputTypeName(Type<?> clazz) {
        return StringUtils.uncapitalize(toOutputTypeName(clazz));
    }

    public String toTopicName(Type<?> clazz) {
        return toOutputTypeName(clazz) + "Topic";
    }

    public String toNonElideInputTypeName(Type<?> clazz) {
        return StringUtils.uncapitalize(toInputTypeName(clazz));
    }
}
