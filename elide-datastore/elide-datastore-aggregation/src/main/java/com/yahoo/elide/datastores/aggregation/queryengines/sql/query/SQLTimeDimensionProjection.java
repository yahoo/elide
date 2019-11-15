/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLColumn;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import lombok.Getter;

/**
 * Represents a projected sql time column for a specific time grain as an alias in a query.
 */
public class SQLTimeDimensionProjection extends SQLColumnProjection implements TimeDimensionProjection {
    private final TimeDimension timeDimension;

    @Getter
    private final TimeGrain grain;

    /**
     * Constructor.
     *
     * @param column The projected sql column
     * @param columnAlias The alias to project this column out.
     * @param timeDimension The logical time dimension
     * @param grain The requested time grain
     */
    public SQLTimeDimensionProjection(SQLColumn column,
                                      String columnAlias,
                                      TimeDimension timeDimension,
                                      TimeGrain grain) {
        super(column, columnAlias);
        this.timeDimension = timeDimension;
        this.grain = grain;
    }

    @Override
    public TimeDimension getTimeDimension() {
        return this.timeDimension;
    }

    /**
     * Returns a String that identifies this time dimension in a SQL query.
     *
     * @return Something like "grain(table_alias.column_name)"
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
