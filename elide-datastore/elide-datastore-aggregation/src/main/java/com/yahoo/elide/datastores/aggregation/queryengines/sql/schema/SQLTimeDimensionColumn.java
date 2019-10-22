/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.schema;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import com.google.common.base.Preconditions;

import java.util.Set;
import java.util.TimeZone;

/**
 * A time dimension that supports special sauce needed to generate SQL.
 * This dimension will be created by the SQLQueryEngine in place of a plain TimeDimension.
 */
public class SQLTimeDimensionColumn extends SQLDimensionColumn implements TimeDimensionColumn {
    /**
     * Constructor.
     *
     * @param dimension   a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias  The table alias in SQL where this dimension lives.
     */
    public SQLTimeDimensionColumn(TimeDimensionColumn dimension, String columnAlias, String tableAlias) {
        super(dimension, columnAlias, tableAlias);
    }

    /**
     * Constructor.
     *
     * @param dimension   a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias  The table alias in SQL where this dimension lives.
     * @param joinPath    A '.' separated path through the entity relationship graph that describes
     *                    how to join the time dimension into the current AnalyticView.
     */
    public SQLTimeDimensionColumn(TimeDimensionColumn dimension, String columnAlias, String tableAlias, Path joinPath) {
        super(dimension, columnAlias, tableAlias, joinPath);
    }

    @Override
    public TimeZone getTimeZone() {
        return ((TimeDimensionColumn) wrapped).getTimeZone();

    }

    @Override
    public Set<TimeGrainDefinition> getSupportedGrains() {
        return ((TimeDimensionColumn) wrapped).getSupportedGrains();
    }

    /**
     * Returns a String that identifies this dimension in a SQL query.
     * @param requestedGrain The requested time grain.
     *
     * @return Something like "table_alias.column_name"
     */
    public String getColumnReference(TimeGrain requestedGrain) {
        Preconditions.checkArgument(getSupportedGrains().stream()
                .anyMatch((grainDef -> grainDef.grain().equals(requestedGrain))));

        TimeGrainDefinition definition = getSupportedGrains().stream()
                .filter(grainDef -> grainDef.grain().equals(requestedGrain)).findFirst().get();

        return String.format(definition.expression(), getColumnReference());
    }
}
