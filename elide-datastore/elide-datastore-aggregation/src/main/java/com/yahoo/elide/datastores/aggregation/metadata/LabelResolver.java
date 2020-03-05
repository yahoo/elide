/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import static com.yahoo.elide.datastores.aggregation.core.JoinPath.extendJoinPath;
import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.isPhysicalReference;
import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.resolveFormulaReferences;
import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.toFormulaReference;
import static com.yahoo.elide.utils.TypeHelper.extendTypeAlias;
import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LabelResolver is an interface for resolving column into some type of "labels" such as column reference, join
 * path and so on. It uses Depth-First-Search approach to traverse join path vertically. The resolved results would
 * be stored for quick access.
 */
public abstract class LabelResolver {
    private final Column toResolve;

    protected LabelResolver(Column toResolve) {
        this.toResolve = toResolve;
    }

    /**
     * Resolve label for this column
     *
     * @param labelStore source-of-truth store
     * @param tableAlias label prefix to the table that contains this column
     * @return resolved label
     */
    public abstract String resolveLabel(LabelStore labelStore, String tableAlias);

    /**
     * Get all joins needs for this column
     *
     * @param labelStore source-of-truth store
     * @param from root join path to this column
     * @return full join paths
     */
    public Set<JoinPath> resolveJoinPaths(LabelStore labelStore, JoinPath from) {
        return Collections.singleton(from);
    }

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
        return "Formula reference loop found: "
                + visited.stream()
                        .map(labelResolver -> labelResolver.toResolve.getId())
                        .collect(Collectors.joining("->"))
                + "->" + loop.toResolve.getId();
    }

    /**
     * Get a {@link DimensionFormula} or {@link MetricFormula} reference resolver.
     *
     * @param column column that this resolver is built for
     * @param tableClass table class
     * @param expression formula expression contains physical column, logical column and {@link JoinTo} paths
     * @return a resolver
     */
    public static LabelResolver getFormulaResolver(Column column, Class<?> tableClass, String expression) {
        // dimension references are deduplicated
        List<String> references =
                resolveFormulaReferences(expression).stream().distinct().collect(Collectors.toList());

        return new LabelResolver(column) {
            @Override
            public Set<JoinPath> resolveJoinPaths(LabelStore labelStore, JoinPath from) {
                return references.stream()
                        .map(reference -> {
                            // physical columns don't have dependency resolvers
                            if (isPhysicalReference(tableClass, reference, labelStore.getDictionary())) {
                                return Stream.<JoinPath>empty();
                            }

                            JoinPath to = getJoinToPath(labelStore, reference);

                            return labelStore.getLabelResolver(to)
                                    .resolveJoinPaths(labelStore, extendJoinPath(from, to)).stream();
                        })
                        .reduce(Stream.empty(), Stream::concat)
                        .collect(Collectors.toSet());
            }

            @Override
            public Set<LabelResolver> getDependencyResolvers(LabelStore labelStore) {
                return references.stream()
                        .map(reference -> {
                            // physical columns don't have dependency resolvers
                            return isPhysicalReference(tableClass, reference, labelStore.getDictionary())
                                    ? null
                                    : labelStore.getLabelResolver(getJoinToPath(labelStore, reference));

                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            @Override
            public String resolveLabel(LabelStore labelStore, String tableAlias) {
                String expr = expression;

                // replace references with resolved statements/expressions
                for (String reference : references) {
                    String resolvedReference;

                    if (isPhysicalReference(tableClass, reference, labelStore.getDictionary())) {
                        // physical reference, just append to prefix
                        resolvedReference = getFieldAlias(tableAlias, reference);
                    } else {
                        JoinPath joinPath = getJoinToPath(labelStore, reference);

                        resolvedReference = labelStore.resolveLabel(joinPath, extendTypeAlias(tableAlias, joinPath));
                    }

                    expr = expr.replace(toFormulaReference(reference), resolvedReference);
                }

                return expr;
            }

            private JoinPath getJoinToPath(LabelStore labelStore, String reference) {
                return new JoinPath(tableClass, labelStore.getDictionary(), reference);
            }
        };
    }
}
