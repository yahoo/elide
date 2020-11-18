/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.google.common.collect.ImmutableList;

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
        pathElements = resolvePathElements(entityClass, dictionary, dotSeparatedPath);
    }

    @Override
    protected boolean needNavigation(Class<?> entityClass, String fieldName, EntityDictionary dictionary) {
        return dictionary.isRelation(entityClass, fieldName)
                || MetaDataStore.isTableJoin(entityClass, fieldName, dictionary);
    }

    /**
     * Extend this path with a extension dot separated path
     *
     * @param extensionPath extension path append to this join path
     * @param dictionary dictionary
     * @return expended join path e.g. <code>[A.B]/[B.C] + C.D = [A.B]/[B.C]/[C.D]</code>
     */
    public JoinPath extend(String extensionPath, EntityDictionary dictionary) {
        return extendJoinPath(this, new JoinPath(lastElement().get().getType(), dictionary, extensionPath));
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
    private static <P extends Path> JoinPath extendJoinPath(Path path, P extension) {
        List<Path.PathElement> toExtend = new ArrayList<>(path.getPathElements());
        toExtend.remove(toExtend.size() - 1);
        toExtend.addAll(extension.getPathElements());
        return new JoinPath(toExtend);
    }

    /**
     * Resolve a dot separated path into list of path elements.
     *
     * @param entityClass root class e.g. "foo"
     * @param dictionary dictionary
     * @param dotSeparatedPath path e.g. "bar.baz"
     * @return list of path elements e.g. ["foo.bar", "bar.baz"]
     */
    private List<PathElement> resolvePathElements(Class<?> entityClass,
                                                  EntityDictionary dictionary,
                                                  String dotSeparatedPath) {
        List<PathElement> elements = new ArrayList<>();
        String[] fieldNames = dotSeparatedPath.split("\\.");

        Class<?> currentClass = entityClass;
        for (String fieldName : fieldNames) {
            if (needNavigation(currentClass, fieldName, dictionary)) {
                Class<?> joinClass = dictionary.getParameterizedType(currentClass, fieldName);
                elements.add(new PathElement(currentClass, joinClass, fieldName));
                currentClass = joinClass;
            } else {
                elements.add(resolvePathAttribute(currentClass, dictionary, fieldName));
            }
        }

        return ImmutableList.copyOf(elements);
    }

    private PathElement resolvePathAttribute(Class<?> entityClass,
                                             EntityDictionary dictionary,
                                             String fieldName) {
        Class<?> attributeClass = Object.class;
        if (dictionary.isAttribute(entityClass, fieldName)
                        || fieldName.equals(dictionary.getIdFieldName(entityClass))) {
            attributeClass = dictionary.getType(entityClass, fieldName);
        }
        return new PathElement(entityClass, attributeClass, fieldName);
    }
}
