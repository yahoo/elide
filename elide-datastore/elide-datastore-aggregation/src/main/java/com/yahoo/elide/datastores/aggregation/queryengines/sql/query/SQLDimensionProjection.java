/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * Dimension projection that can expand the dimension into a SQL projection fragment.
 */
@Value
@Builder
@AllArgsConstructor
public class SQLDimensionProjection implements SQLColumnProjection {
    private String name;
    private ValueType valueType;
    private ColumnType columnType;
    private String expression;
    private String alias;
    private Map<String, Argument> arguments;
    private boolean projected;

    public SQLDimensionProjection(Dimension dimension,
                                  String alias,
                                  Map<String, Argument> arguments,
                                  boolean projected) {
        this.name = dimension.getName();
        this.expression = dimension.getExpression();
        this.valueType = dimension.getValueType();
        this.columnType = dimension.getColumnType();
        this.alias = alias;
        this.arguments = arguments;
        this.projected = projected;
    }

    @Override
    public boolean canNest(Queryable source, SQLReferenceTable lookupTable) {
        return true;
    }

    @Override
    public ColumnProjection outerQuery(Queryable source, SQLReferenceTable lookupTable, boolean joinInOuter) {
        /*
         * Default Behiavior:
         * - Dimensions without joins: everything in inner query.  Alias reference in outer query.
         * - Dimensions with joins: Physical columns projected in inner query.  Everything else applied post agg.
         * - Outer columns are virtual if they only appear in HAVING, WHERE, or SORT.
         */
        Set<SQLColumnProjection> joinProjections = lookupTable.getResolvedJoinProjections(source.getSource(), name);

        boolean requiresJoin = joinProjections.size() > 0;

        boolean inProjection = source.getColumnProjection(name) != null;

        if (requiresJoin && joinInOuter) {
            return SQLDimensionProjection.builder()
                    .name(name)
                    .alias(alias)
                    .valueType(valueType)
                    .columnType(columnType)
                    .expression(expression)
                    .arguments(arguments)
                    .projected(inProjection)
                    .build();
        } else {
            return SQLDimensionProjection.builder()
                    .name(name)
                    .alias(alias)
                    .valueType(valueType)
                    .columnType(columnType)
                    .expression("{{" + this.getSafeAlias() + "}}")
                    .arguments(arguments)
                    .projected(true)
                    .build();
        }
    }

    @Override
    public boolean isProjected() {
        return projected;
    }
}
