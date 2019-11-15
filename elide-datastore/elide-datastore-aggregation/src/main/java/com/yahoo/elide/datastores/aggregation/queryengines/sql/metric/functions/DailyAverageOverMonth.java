/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.BasicSQLMetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;
import com.yahoo.elide.request.Argument;

import java.util.Collections;
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
                                    Metric metric,
                                    String alias,
                                    Set<ColumnProjection> dimensions,
                                    TimeDimensionProjection timeDimension) {
        try {
            SQLQueryTemplate subQuery = SqlSum.class.newInstance().resolve(
                    arguments,
                    metric,
                    alias,
                    dimensions,
                    timeDimension.toTimeGrain(TimeGrain.DAY));

            return SqlAvg.class.newInstance().resolve(
                    arguments,
                    subQuery.getMetrics().get(0),
                    alias,
                    subQuery.toTimeGrain(TimeGrain.MONTH));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InternalServerErrorException("Can't construct subquery template for " + getName() + ".");
        }
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                             MetricFunctionInvocation metric,
                                             String alias,
                                             SQLQueryTemplate subQuery) {
        throw new InvalidOperationException("Can't apply aggregation " + getName() + " on nested query.");
    }
}
