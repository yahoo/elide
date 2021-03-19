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
import java.util.Set;

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
    default ColumnProjection outerQuery() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Set<ColumnProjection> innerQuery() {
        return new HashSet<>(Arrays.asList(this));
    }

    default boolean isVirtual() {
        return false;
    }
}
