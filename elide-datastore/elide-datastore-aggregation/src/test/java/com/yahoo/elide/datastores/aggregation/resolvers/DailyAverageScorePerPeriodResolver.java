/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.resolvers;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;

import java.util.HashMap;
import java.util.Map;

/**
 * Query plan resolver for the dailyAverageScorePerPeriod metric in the PlayerStats table.
 */
public class DailyAverageScorePerPeriodResolver implements QueryPlanResolver {

    @Override
    public QueryPlan resolve(Query query, MetricProjection projection) {
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
                        arguments, false
                )).build();

        QueryPlan outerQuery = QueryPlan.builder()
                .source(innerQuery)
                .metricProjection(SQLMetricProjection.builder()
                        .alias(projection.getAlias())
                        .name(projection.getName())
                        .expression(projection.getExpression())
                        .columnType(projection.getColumnType())
                        .valueType(projection.getValueType())
                        .arguments(projection.getArguments())
                        .build())
                .build();

        return outerQuery;
    }
}
