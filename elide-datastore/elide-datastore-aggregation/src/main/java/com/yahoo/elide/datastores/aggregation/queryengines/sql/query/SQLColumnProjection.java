/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
public interface SQLColumnProjection extends ColumnProjection {

    /**
     * Generate a SQL fragment for this combination column and client arguments.
     * @param source the queryable that contains the column.
     * @param table symbol table to resolve column name references.
     * @return
     */
    default String toSQL(Queryable source, SQLReferenceTable table) {
        return table.getResolvedReference(source, getName());
    }

    @Override
    default boolean canNest() {
        return false;
    }

    @Override
    default ColumnProjection outerQuery(SQLReferenceTable lookupTable) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<ColumnProjection> innerQuery(SQLReferenceTable lookupTable) {
        return new HashSet<>(Arrays.asList(this));
    }

    /**
     * Returns whether or not this column is projected in the output (included in SELECT) or
     * only referenced in a filter expression.
     * @return True if part of the output projection.  False otherwise.
     */
    default boolean isProjected() {
        return true;
    }

    /**
     * Nests a set of column projections returning the outer query equivalent.
     * @param columns The set of columns to nest.
     * @param lookupTable answers questions that require template resolution.
     * @param <T> The column projection type.
     * @return a set of column projections that have been nested.
     */
    public static <T extends ColumnProjection> Set<T> outerQueryProjections(Set<T> columns,
                                                                            SQLReferenceTable lookupTable) {
        return (Set<T>) columns.stream()
                .map(SQLColumnProjection.class::cast)
                .map(projection -> projection.outerQuery(lookupTable))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Nests a set of column projections returning the inner query equivalents.
     * @param columns The set of columns to nest.
     * @param lookupTable answers questions that require template resolution.
     * @param <T> The column projection type.
     * @return a set of column projections that have been nested.
     */
    public static <T extends ColumnProjection> Set<T> innerQueryProjections(Set<T> columns,
                                                                            SQLReferenceTable lookupTable) {
        return (Set<T>) columns.stream()
                .map(SQLColumnProjection.class::cast)
                .flatMap(projection -> projection.innerQuery(lookupTable).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
