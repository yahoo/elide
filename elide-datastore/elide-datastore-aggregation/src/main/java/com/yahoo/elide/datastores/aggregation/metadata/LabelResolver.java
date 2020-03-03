/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LabelResolver is an interface for resolving column into some type of "labels" such as column reference, join
 * path and so on. It uses Depth-First-Search approach to traverse join path vertically. The resolved results would
 * be stored for quick access.
 */
public abstract class LabelResolver {
    private final Column column;

    public LabelResolver(Column column) {
        this.column = column;
    }

    /**
     * Resolve a label for a join path. This method need to be implemented for each column.
     *
     * @param fromPath path to be resolved
     * @param generator generator to construct labels
     * @param labelStore table stores all resolvers
     * @param <T> label value type
     * @return resolved label
     */
    public abstract <T> T resolveLabel(JoinPath fromPath, LabelGenerator<T> generator, LabelStore labelStore);

    /**
     * Get all other resolvers that this resolver would involve when resolving label.
     *
     * @param labelStore table stores all resolvers
     * @return dependency resolvers
     */
    protected Set<LabelResolver> getDependencyResolvers(LabelStore labelStore) {
        return Collections.emptySet();
    }

    /**
     * Check whether this resolver would cause reference loop
     *
     * @param metaDataStore meta data store
     */
    public void checkResolverLoop(MetaDataStore metaDataStore) {
        this.checkResolverLoop(new LinkedHashSet<>(), metaDataStore);
    }

    /**
     * Check whether this resolver would cause reference loop
     *
     * @param visited visited label resolvers
     * @param metaDataStore meta data store
     */
    private void checkResolverLoop(LinkedHashSet<LabelResolver> visited, MetaDataStore metaDataStore) {
        if (visited.contains(this)) {
            throw new IllegalArgumentException(referenceLoopMessage(visited, this));
        } else {
            visited.add(this);
            this.getDependencyResolvers(metaDataStore)
                    .forEach(resolver -> resolver.checkResolverLoop(visited, metaDataStore));
            visited.remove(this);
        }
    }

    /**
     * Construct reference loop message.
     */
    private static String referenceLoopMessage(LinkedHashSet<LabelResolver> visited, LabelResolver loop) {
        return "Dimension formula reference loop found: "
                + visited.stream().map(labelResolver -> labelResolver.column.getId()).collect(Collectors.joining("->"))
                + "->" + loop.column.getId();
    }

    /**
     * LabelGenerator is an interface that convert a [column, reference] pair into some other types of value.
     *
     * @param <T> label value type
     */
    @FunctionalInterface
    public interface LabelGenerator<T> {
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
