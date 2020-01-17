/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;

import java.util.List;

/**
 * JoinPath extends {@link Path} to allow navigation through {@link Join} annotation.
 */
public class JoinPath extends Path {
    public JoinPath(Path other) {
        this(other.getPathElements());
    }

    public JoinPath(List<PathElement> pathElements) {
        super(pathElements);
    }

    public JoinPath(Class<?> entityClass, EntityDictionary dictionary, String dotSeparatedPath) {
        super(entityClass, dictionary, dotSeparatedPath);
    }

    @Override
    protected boolean needNavigation(Class<?> entityClass, String fieldName, EntityDictionary dictionary) {
        return dictionary.isRelation(entityClass, fieldName)
                || MetaDataStore.isTableJoin(entityClass, fieldName, dictionary);
    }
}
