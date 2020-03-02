/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LabelResolver is an interface for resolving column into some type of "labels" such as column reference, join
 * path and so on. It uses Depth-First-Search approach to traverse join path vertically. The resolved results would
 * be stored for quick access.
 */
public interface LabelResolver {
    /**
     * Resolve a label for a join path. This method need to be implemented for each column.
     *
     * @param fromPath path to be resolved
     * @param toResolve paths that are pending resolving
     * @param resolved resolved paths
     * @param generator generator to construct labels
     * @param metaDataStore meta data store
     * @param <T> label value type
     * @return resolved label
     */
    <T> T resolveLabel(JoinPath fromPath,
                       Set<JoinPath> toResolve,
                       Map<JoinPath, T> resolved,
                       LabelGenerator<T> generator,
                       MetaDataStore metaDataStore);

    /**
     * DFS recursion method for constructing label for a reference field in a class.
     *
     * @param fromPath path to a reference that needs to be resolved
     * @param tableClass table class of that reference
     * @param reference reference
     * @param toResolve paths that are pending resolving
     * @param resolved resolved paths
     * @param generator generator to construct labels
     * @param metaDataStore meta data store
     * @param <T> label value type
     * @return resolved label for this reference
     */
    static <T> T resolveReference(JoinPath fromPath,
                                  Class<?> tableClass,
                                  String reference,
                                  Set<JoinPath> toResolve,
                                  Map<JoinPath, T> resolved,
                                  LabelGenerator<T> generator,
                                  MetaDataStore metaDataStore) {
        if (toResolve.contains(fromPath)) {
            throw new IllegalArgumentException(
                    referenceLoopMessage(tableClass, fromPath, toResolve, metaDataStore.getDictionary()));
        }

        toResolve.add(fromPath);

        JoinPath extension = new JoinPath(tableClass, metaDataStore.getDictionary(), reference);

        // append new path after original path
        JoinPath extended = extendJoinPath(fromPath, extension);

        // the extension fragment also need to be marked as not resolved as to prevent infinite
        // appending like A.B.B.B...
        if (!resolved.containsKey(extended)) {
            if (!extended.equals(extension)) {
                if (toResolve.contains(extension)) {
                    throw new IllegalArgumentException(
                            referenceLoopMessage(tableClass, fromPath, toResolve, metaDataStore.getDictionary()));
                }
                toResolve.add(extension);
            }

            resolved.put(extended, metaDataStore.resolveLabel(extended, toResolve, resolved, generator));
        }

        return resolved.get(extended);
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
    static <P extends Path> JoinPath extendJoinPath(Path path, P extension) {
        List<Path.PathElement> toExtend = new ArrayList<>(path.getPathElements());
        toExtend.remove(toExtend.size() - 1);
        toExtend.addAll(extension.getPathElements());
        return new JoinPath(toExtend);
    }

    /**
     * Construct reference loop message.
     */
    static String referenceLoopMessage(Class<?> tableClass,
                                       Path path,
                                       Set<? extends Path> toResolve,
                                       EntityDictionary dictionary) {
        return "Dimension formula reference loop found in class "
                + dictionary.getJsonAliasFor(tableClass) + ": "
                + toResolve.stream().map(Path::toString).collect(Collectors.joining("->"))
                + "->" + path.toString();
    }

    /**
     * LabelGenerator is an interface that convert a column and refernce pair into some other types of value.
     *
     * @param <T> label value type
     */
    @FunctionalInterface
    interface LabelGenerator<T> {
        /**
         * Generate a "label" for given path and reference
         *
         * @param path path to a field
         * @param reference reference that represent this field, e.g. physical column name
         * @return generated label
         */
        T apply(JoinPath path, String reference);

        /**
         * Convert reference to "label"
         *
         * @param reference reference that represent this field, e.g. physical column name
         * @return generated label
         */
        default T apply(String reference) {
            return apply(null, reference);
        }
    }
}
