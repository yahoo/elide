/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.resolvers;

import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;

import java.lang.annotation.Annotation;

/**
 * Query plan resolver for the dailyAverageScorePerPeriod metric in the PlayerStats table.
 */
public class DailyAverageScorePerPeriodResolver implements QueryPlanResolver {

    @Override
    public QueryPlan resolve(MetricProjection projection) {

        //Ideally, these would be sourced from the SQLTable instead of reconstructed here:
        MetricProjection innerMetric = SQLMetricProjection
                .builder()
                .source(projection.getSource())
                .valueType(ValueType.INTEGER)
                .columnType(ColumnType.FORMULA)
                .expression("{{highScore}}")
                .name("highScore")
                .alias("highScore")
                .build();

        TimeDimensionProjection innerTimeGrain = SQLTimeDimensionProjection
                .builder()
                .source(projection.getSource())
                .valueType(ValueType.TIME)
                .columnType(ColumnType.FORMULA)
                .expression("{{recordedDate}}")
                .name("recordedDate")
                .alias("recordedDate")
                .grain(new TimeDimensionGrain("recordedDate", new TimeGrainDefinition() {

                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return TimeGrainDefinition.class;
                    }

                    @Override
                    public TimeGrain grain() {
                        return TimeGrain.SIMPLEDATE;
                    }

                    @Override
                    public String expression() {
                        return PlayerStats.DATE_FORMAT;
                    }
                }))
                .build();

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
