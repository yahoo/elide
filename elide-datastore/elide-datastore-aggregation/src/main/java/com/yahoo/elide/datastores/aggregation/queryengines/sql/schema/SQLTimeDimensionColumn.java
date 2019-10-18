/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.schema;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;

import java.util.Set;
import java.util.TimeZone;

/**
 * A time dimension that supports special sauce needed to generate SQL.
 * This dimension will be created by the SQLQueryEngine in place of a plain TimeDimension.
 */
public class SQLTimeDimensionColumn extends SQLDimensionColumn implements TimeDimensionColumn {
    /**
     * Constructor.
     * @param dimension a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     */
    public SQLTimeDimensionColumn(TimeDimensionColumn dimension, String columnAlias, String tableAlias) {
        super(dimension, columnAlias, tableAlias);
    }

    /**
     * Constructor.
     * @param dimension a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     * @param joinPath A '.' separated path through the entity relationship graph that describes
     *                 how to join the time dimension into the current AnalyticView.
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
}
