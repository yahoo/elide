/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.request.Argument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Metric projection that can expand the metric into a SQL projection fragment.
 */
@Value
@Builder
@AllArgsConstructor
public class SQLMetricProjection implements MetricProjection, SQLColumnProjection {
    private Queryable source;
    private String name;
    private ValueType valueType;
    private ColumnType columnType;
    private String expression;
    private String alias;
    private Map<String, Argument> arguments;
    private QueryPlanResolver queryPlanResolver;

    @Override
    public QueryPlan resolve() {
        if (queryPlanResolver != null) {
            return queryPlanResolver.resolve(this);
        }
        return MetricProjection.super.resolve();
    }

    public SQLMetricProjection(Metric metric,
                               String alias,
                               Map<String, Argument> arguments) {
        this.queryPlanResolver = null;
        this.source = (SQLTable) metric.getTable();
        this.name = metric.getName();
        this.expression = metric.getExpression();
        this.valueType = metric.getValueType();
        this.columnType = metric.getColumnType();
        this.alias = alias;
        this.arguments = arguments;
    }
}
