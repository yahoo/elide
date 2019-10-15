/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine.schema;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import java.util.TimeZone;

/**
 * A time dimension that supports special sauce needed to generate SQL.
 * This dimension will be created by the SQLQueryEngine in place of a plain TimeDimension.
 */
public class SQLTimeDimension extends SQLDimension implements TimeDimension {

    /**
     * Constructor
     * @param dimension a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     */
    public SQLTimeDimension(Dimension dimension, String columnAlias, String tableAlias) {
        super(dimension, columnAlias, tableAlias);
    }

    /**
     * Constructor
     * @param dimension a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     * @param joinPath A '.' separated path through the entity relationship graph that describes
     *                 how to join the time dimension into the current AnalyticView.
     */
    public SQLTimeDimension(Dimension dimension, String columnAlias, String tableAlias, Path joinPath) {
        super(dimension, columnAlias, tableAlias, joinPath);
    }

    @Override
    public TimeZone getTimeZone() {
        //TODO
        return null;

    }

    @Override
    public TimeGrain getTimeGrain() {
        //TODO
        return null;
    }
}
