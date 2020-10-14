/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
public interface SQLColumnProjection extends ColumnProjection {

    /**
     * Generate a SQL fragment for this combination column and client arguments.
     * @param table symbol table to resolve column name references.
     * @return
     */
    default String toSQL(SQLReferenceTable table) {
        return table.getResolvedReference(getSource(), getName());
    }

    /**
     * Makes a copy of this column with a new source.
     * @param source The new source.
     * @return copy of the column projection.
     */
    default SQLColumnProjection withSource(Queryable source) {
        throw new UnsupportedOperationException();
    }

    /**
     * Makes a copy of this column with a new source and expression.
     * @param source The new source.
     * @param expression The new expression.
     * @return copy of the column projection.
     */
    default SQLColumnProjection withSourceAndExpression(Queryable source, String expression) {
        throw new UnsupportedOperationException();
    }

    /**
     * Makes of a copy of a set of columns all with a new source.
     * @param source The new source.
     * @param columns The columns to copy.
     * @param <T> The column projection type.
     * @return An ordered set of the column copies.
     */
    static <T extends ColumnProjection> Set<T> withSource(Queryable source, Set<T> columns)  {
        return (Set<T>) columns.stream()
                .map(SQLColumnProjection.class::cast)
                .map(column -> column.withSource(source))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Makes of a copy of a set of columns all with a new source.
     * @param source The new source.
     * @param columns The columns to copy.
     * @param <T> The column projection type.
     * @return An ordered set of the column copies.
     */
    static <T extends ColumnProjection> Set<T> withSourceAndExpression(Queryable source,
                                                                       Set<T> columns)  {
        return (Set<T>) columns.stream()
                .map(SQLColumnProjection.class::cast)
                .map(column -> column.withSourceAndExpression(source, "{{" + column.getName() + "}}"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
