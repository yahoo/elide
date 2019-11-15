/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatableField;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.BasicSQLMetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;
import com.yahoo.elide.request.Argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Daily average value of a metric monthly.
 * Would create a query to calculate the daily sum, and then create a query to calculate the monthly average.
 */
public class DailyAverageOverMonth extends BasicSQLMetricFunction {
    public DailyAverageOverMonth() {
        super(
                "dailyAvgOverMonth",
                "daily average over month",
                "daily average of a metric over months",
                Collections.emptySet());
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    List<AggregatableField> fields,
                                    String alias,
                                    Set<ColumnProjection> dimensions,
                                    TimeDimensionProjection timeDimension) {
        try {
            SQLQueryTemplate subQuery = SqlSum.class.newInstance().resolve(
                    arguments,
                    fields,
                    alias,
                    dimensions,
                    timeDimension.toTimeGrain(TimeGrain.DAY));

            return SqlAvg.class.newInstance().resolve(
                    arguments,
                    new ArrayList<>(subQuery.getMetrics()),
                    alias,
                    subQuery.toTimeGrain(TimeGrain.MONTH));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InternalServerErrorException("Can't construct subquery template for " + getName() + ".");
        }
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    List<AggregatableField> fields,
                                    String alias,
                                    SQLQueryTemplate subQuery) {
        throw new InvalidOperationException("Can't apply aggregation " + getName() + " on nested query.");
    }
}
