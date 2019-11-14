/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

/**
 * A time dimension that supports special sauce needed to generate SQL.
 * This dimension will be created by the SQLQueryEngine in place of a plain TimeDimension.
 */
public class SQLTimeDimensionProjection extends SQLDimensionProjection {
    private final TimeDimension timeDimension;
    private final TimeGrain grain;

    /**
     * Constructor.
     *
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     * @param joinPath A '.' separated path through the entity relationship graph that describes
     *                 how to join the time dimension into the current AnalyticView.
     * @param timeDimension The logical time dimension
     * @param grain The requested time grain
     */
    public SQLTimeDimensionProjection(String columnAlias,
                                      String tableAlias,
                                      Path joinPath,
                                      TimeDimension timeDimension,
                                      TimeGrain grain) {
        super(columnAlias, tableAlias, columnAlias, joinPath);
        this.timeDimension = timeDimension;
        this.grain = grain;
    }

    /**
     * Returns a String that identifies this dimension in a SQL query.
     *
     * @return Something like "table_alias.column_name"
     */
    @Override
    public String getColumnReference() {
        TimeDimensionGrain grainInfo = timeDimension.getSupportedGrains().stream()
                .filter(g -> g.getGrain().equals(grain))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Requested time grain not supported."));

        //TODO - We will likely migrate to a templating language when we support parameterized metrics.
        return String.format(grainInfo.getExpression(), super.getColumnReference());
    }
}
