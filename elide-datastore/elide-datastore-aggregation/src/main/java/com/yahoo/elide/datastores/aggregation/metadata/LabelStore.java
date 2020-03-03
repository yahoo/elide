/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.LabelResolver.LabelGenerator;

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
     * Get the label for a path using provided label generator.
     *
     * @param path path to a field
     * @param generator function for generating labels
     * @param <T> label value type
     * @return generated label
     */
    default <T> T generateLabel(JoinPath path, LabelGenerator<T> generator) {
        return getLabelResolver(path).resolveLabel(path, generator, this);
    }

    /**
     * Get the label for a path using provided label generator.
     *
     * @param path path to a field
     * @param generator function for generating labels
     * @param <T> label value type
     * @return generated label
     */
    default <T> T generateLabel(Path path, LabelGenerator<T> generator) {
        return generateLabel(new JoinPath(path), generator);
    }
}
