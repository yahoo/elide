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

import java.util.ArrayList;
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

    /**
     * Append an extension path to an original path, the last element of original path should be the same as the
     * first element of extension path.
     *
     * @param path original path, e.g. <code>[A.B]/[B.C]</code>
     * @param extension extension path, e.g. <code>[B.C]/[C.D]</code>
     * @param <P> path extension
     * @return extended path <code>[A.B]/[B.C]/[C.D]</code>
     */
    public static <P extends Path> JoinPath extendJoinPath(Path path, P extension) {
        List<Path.PathElement> toExtend = new ArrayList<>(path.getPathElements());
        toExtend.remove(toExtend.size() - 1);
        toExtend.addAll(extension.getPathElements());
        return new JoinPath(toExtend);
    }
}
