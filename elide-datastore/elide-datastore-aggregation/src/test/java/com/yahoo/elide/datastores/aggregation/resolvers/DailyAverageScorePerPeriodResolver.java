/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.resolvers;

import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;

/**
 * Query plan resolver for the dailyAverageScorePerPeriod metric in the PlayerStats table.
 */
public class DailyAverageScorePerPeriodResolver implements QueryPlanResolver {

    @Override
    public QueryPlan resolve(MetricProjection projection) {
        SQLTable table = (SQLTable) projection.getSource();

        MetricProjection innerMetric = table.getMetricProjection("highScore");
        TimeDimensionProjection innerTimeGrain = table.getTimeDimensionProjection("recordedDate");

        QueryPlan innerQuery = QueryPlan.builder()
                .source(projection.getSource())
                .metricProjection(innerMetric)
                .timeDimensionProjection(innerTimeGrain)
                .build();

        QueryPlan outerQuery = QueryPlan.builder()
                .source(innerQuery)
                .metricProjection(SQLMetricProjection.builder()
                        .source(innerQuery)
                        .alias(projection.getAlias())
                        .name(projection.getName())
                        .expression(projection.getExpression())
                        .columnType(projection.getColumnType())
                        .valueType(projection.getValueType())
                        .build())
                .build();

        return outerQuery;
    }
}
