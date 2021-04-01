/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.custom;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom metric projection for daily average score per time period.
 */
public class DailyAverageScorePerPeriod extends SQLMetricProjection {

    public DailyAverageScorePerPeriod(Metric metric,
                                      String alias,
                                      Map<String, Argument> arguments) {
        super(metric.getName(), metric.getValueType(), metric.getColumnType(), metric.getExpression(),
                alias, arguments, true);
    }

    @Override
    public QueryPlan resolve(Query query) {
        SQLTable table = (SQLTable) query.getSource();

        MetricProjection innerMetric = table.getMetricProjection("highScore");
        TimeDimension innerTimeGrain = table.getTimeDimension("recordedDate");
        Map<String, Argument> arguments = new HashMap<>();
        arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.DAY).build());

        QueryPlan innerQuery = QueryPlan.builder()
                .source(query.getSource())
                .metricProjection(innerMetric)
                .timeDimensionProjection(new SQLTimeDimensionProjection(
                        innerTimeGrain,
                        innerTimeGrain.getTimezone(),
                        "recordedDate_DAY",
                        arguments, true
                )).build();

        QueryPlan outerQuery = QueryPlan.builder()
                .source(innerQuery)
                .metricProjection(SQLMetricProjection.builder()
                        .alias(getAlias())
                        .name(getName())
                        .expression(getExpression())
                        .columnType(getColumnType())
                        .valueType(getValueType())
                        .arguments(getArguments())
                        .build())
                .build();

        return outerQuery;
    }
}
