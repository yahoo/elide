/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
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
     * @param lookupTable symbol table to resolve column name references.
     * @return SQL query String for this column
     */
    default String toSQL(Queryable source, SQLReferenceTable lookupTable) {
        return lookupTable.getResolvedReference(source, getName());
    }

    @Override
    default boolean canNest(Queryable source, SQLReferenceTable lookupTable) {
        return false;
    }

    @Override
    default Pair<ColumnProjection, Set<ColumnProjection>> nest(Queryable source,
                                                              SQLReferenceTable lookupTable,
                                                              boolean joinInOuter) {

        Set<SQLColumnProjection> joinProjections = lookupTable.getResolvedJoinProjections(source.getSource(),
                getName());

        boolean requiresJoin = joinProjections.size() > 0;

        boolean inProjection = source.getColumnProjection(getName()) != null;

        ColumnProjection outerProjection;
        Set<ColumnProjection> innerProjections;

        if (requiresJoin && joinInOuter) {
            outerProjection = withExpression(getExpression(), inProjection);
            innerProjections = joinProjections.stream().collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            outerProjection = withExpression("{{" + this.getSafeAlias() + "}}", isProjected());
            innerProjections = new LinkedHashSet<>(Arrays.asList(this));
        }

        return Pair.of(outerProjection, innerProjections);
    }

    SQLColumnProjection withExpression(String expression, boolean project);

    /**
     * Returns whether or not this column is projected in the output (included in SELECT) or
     * only referenced in a filter expression.
     * @return True if part of the output projection.  False otherwise.
     */
    default boolean isProjected() {
        return true;
    }
}
