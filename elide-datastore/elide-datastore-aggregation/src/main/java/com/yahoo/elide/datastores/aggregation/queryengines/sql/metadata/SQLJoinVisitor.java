/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.ColumnVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

/**
 * SQLJoinVisitor get all required JOINs for a column.
 */
public class SQLJoinVisitor extends ColumnVisitor<Set<JoinPath>> {
    // this visitor is using DFS pattern when traversing columns, use Stack as state tracker
    private final Stack<JoinPath> froms = new Stack<>();

    public SQLJoinVisitor(MetaDataStore metaDataStore) {
        super(metaDataStore);
    }

    @Override
    protected Set<JoinPath> visitFormulaMetric(MetricProjection metric) {
        return visitFormulaColumn(metric);
    }

    /**
     * FIELD column doesn't need JOINs.
     *
     * @param dimension a FIELD dimension
     * @return empty set
     */
    @Override
    protected Set<JoinPath> visitFieldDimension(ColumnProjection dimension) {
        return Collections.emptySet();
    }

    @Override
    protected Set<JoinPath> visitFormulaDimension(ColumnProjection dimension) {
        return visitFormulaColumn(dimension);
    }

    /**
     * Check all references contained in FORMULA column expression and add all JOINs together.
     *
     * @param column a FORMULA column
     * @return all JOINs
     */
    private Set<JoinPath> visitFormulaColumn(ColumnProjection column) {
        Set<JoinPath> joinPaths = new HashSet<>();

        // only need to add joins for references to fields defined in other table
        for (String reference : resolveFormulaReferences(column.getExpression())) {
            if (reference.contains(".")) {
                joinPaths.addAll(visitJoinToReference(column, reference));
            }
        }

        return joinPaths;
    }

    /**
     * Resolve join path reference from a column.
     *
     * @param column from column
     * @param joinToPath a dot separated path
     * @return resolved JOIN paths
     */
    private Set<JoinPath> visitJoinToReference(ColumnProjection column, String joinToPath) {
        Set<JoinPath> joinPaths = new HashSet<>();

        Queryable source = column.getSource();
        JoinPath joinPath = froms.empty()
                ? new JoinPath(dictionary.getEntityClass(source.getName(), source.getVersion()), dictionary, joinToPath)
                : froms.peek().extend(joinToPath, dictionary);
        joinPaths.add(joinPath);

        froms.add(joinPath);
        joinPaths.addAll(Objects.requireNonNull(visitColumn(getColumn(joinPath))));
        froms.pop();

        return joinPaths;
    }
}
