/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.isTableJoin;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.datastores.aggregation.annotation.Join;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * JoinPath extends {@link Path} to allow navigation through {@link Join} annotation.
 */
public class JoinPath extends Path {
    public JoinPath(Class<?> entityClass, EntityDictionary dictionary, String dotSeparatedPath) {
        super(resolveJoinPathElements(entityClass, dictionary, dotSeparatedPath));
    }

    private static List<PathElement> resolveJoinPathElements(Class<?> entityClass,
                                                             EntityDictionary dictionary,
                                                             String dotSeparatedPath) {
        List<PathElement> elements = new ArrayList<>();
        String[] fieldNames = dotSeparatedPath.split("\\.");

        Class<?> currentClass = entityClass;
        for (String fieldName : fieldNames) {
            if (dictionary.isRelation(currentClass, fieldName) || isTableJoin(currentClass, fieldName, dictionary)) {
                Class<?> joinClass = dictionary.getParameterizedType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, joinClass, fieldName));
                currentClass = joinClass;
            } else if (dictionary.isAttribute(currentClass, fieldName)
                    || fieldName.equals(dictionary.getIdFieldName(entityClass))) {
                Class<?> attributeClass = dictionary.getType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, attributeClass, fieldName));
            } else if ("this".equals(fieldName)) {
                elements.add(new PathElement(currentClass, null, fieldName));
            } else {
                String alias = dictionary.getJsonAliasFor(currentClass);
                throw new InvalidValueException(alias + " doesn't contain the field " + fieldName);
            }
        }

        return ImmutableList.copyOf(elements);
    }
}
