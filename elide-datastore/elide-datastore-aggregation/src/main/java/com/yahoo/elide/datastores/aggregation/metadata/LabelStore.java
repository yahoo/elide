/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;

import java.util.Set;

/**
 * LabelStore stores all label resolvers for all columns. It uses {@link JoinPath} as reference to each field.
 */
public interface LabelStore {
    /**
     * Get a dictionary with model definitions.
     *
     * @return dictionary
     */
    EntityDictionary getDictionary();

    /**
     * Get the label resolver for a path
     *
     * @param path path to a logical field
     * @return a label resolver
     */
    LabelResolver getLabelResolver(Path path);

    /**
     * Get the label for a path.
     *
     * @param path path to a field
     * @param tableAlias label prefix to the table that contains this field
     * @return full label for the path
     */
    default String resolveLabel(JoinPath path, String tableAlias) {
        return getLabelResolver(path).resolveLabel(this, tableAlias);
    }

    /**
     * Get the label for a path.
     *
     * @param path path to a field
     * @param tableAlias label prefix to the table that contains this field
     * @return full label for the path
     */
    default String resolveLabel(Path path, String tableAlias) {
        return resolveLabel(new JoinPath(path), tableAlias);
    }

    /**
     * Resolve all joins needed for a path
     *
     * @param path path to a field
     * @return all needed joins
     */
    default Set<JoinPath> resolveJoinPaths(JoinPath path) {
        return getLabelResolver(path).resolveJoinPaths(this, path);
    }
}
