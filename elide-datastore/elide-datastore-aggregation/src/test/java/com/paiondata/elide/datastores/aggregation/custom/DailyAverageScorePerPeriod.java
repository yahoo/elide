/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.custom;

import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.paiondata.elide.datastores.aggregation.metadata.models.Metric;
import com.paiondata.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.paiondata.elide.datastores.aggregation.query.MetricProjection;
import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.query.QueryPlan;
import com.paiondata.elide.datastores.aggregation.query.Queryable;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;

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

    public DailyAverageScorePerPeriod(SQLMetricProjection projection, String expression) {
        super(projection.getName(), projection.getValueType(), projection.getColumnType(), expression,
                        projection.getAlias(), projection.getArguments(), true);
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
                .metricProjection(new DailyAverageScorePerPeriod(this, "AVG({{$highScore}})"))
                .build();

        return outerQuery;
    }

    // TODO: Remove this when switching to TableContext
    // Resolved Reference would be empty if value is not provided in @MetricFormula as value is optional.
    // Once we change this to use TableContext, then it should be able to resolve expression directly.
    @Override
    public boolean canNest(Queryable source, MetaDataStore metaDataStore) {
        return true;
    }
}
