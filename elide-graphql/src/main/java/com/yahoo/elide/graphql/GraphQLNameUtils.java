/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;


import com.yahoo.elide.core.EntityDictionary;

import org.apache.commons.lang3.StringUtils;

public class GraphQLNameUtils {
    private static final String INPUT_SUFFIX = "Input";
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

    public String toEdgesName(Class<?> clazz) {
        return toOutputTypeName(clazz) + EDGE_SUFFIX;
    }
}
