/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

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
public abstract class SQLReferenceResolver {
    private final Column toResolve;

    protected SQLReferenceResolver(Column toResolve) {
        this.toResolve = toResolve;
    }

    /**
     * Resolve label for this column
     *
     * @param referenceTable source-of-truth store
     * @param tableAlias label prefix to the table that contains this column
     * @return resolved label
     */
    public abstract String resolveReference(SQLReferenceTable referenceTable, String tableAlias);

    /**
     * Get all joins needs for this column
     *
     * @param referenceTable source-of-truth store
     * @param from root join path to this column
     * @return full join paths
     */
    public Set<JoinPath> resolveJoinPaths(SQLReferenceTable referenceTable, JoinPath from) {
        return Collections.singleton(from);
    }

    /**
     * Get all other resolvers that this resolver would involve when resolving label.
     *
     * @param referenceTable table stores all resolvers
     * @return dependency resolvers
     */
    protected Set<SQLReferenceResolver> getDependencyResolvers(SQLReferenceTable referenceTable) {
        return Collections.emptySet();
    }

    /**
     * Check whether this resolver would cause reference loop
     *
     * @param referenceTable table stores all SQL reference resolvers
     */
    public void checkResolverLoop(SQLReferenceTable referenceTable) {
        this.checkResolverLoop(new LinkedHashSet<>(), referenceTable);
    }

    /**
     * Check whether this resolver would cause reference loop
     *
     * @param visited visited label resolvers
     * @param referenceTable table stores all SQL reference resolvers
     */
    private void checkResolverLoop(LinkedHashSet<SQLReferenceResolver> visited, SQLReferenceTable referenceTable) {
        if (visited.contains(this)) {
            throw new IllegalArgumentException(referenceLoopMessage(visited, this));
        } else {
            visited.add(this);
            this.getDependencyResolvers(referenceTable)
                    .forEach(resolver -> resolver.checkResolverLoop(visited, referenceTable));
            visited.remove(this);
        }
    }

    /**
     * Construct reference loop message.
     */
    private static String referenceLoopMessage(LinkedHashSet<SQLReferenceResolver> visited, SQLReferenceResolver loop) {
        return "Formula reference loop found: "
                + visited.stream()
                        .map(labelResolver -> labelResolver.toResolve.getId())
                        .collect(Collectors.joining("->"))
                + "->" + loop.toResolve.getId();
    }
}
