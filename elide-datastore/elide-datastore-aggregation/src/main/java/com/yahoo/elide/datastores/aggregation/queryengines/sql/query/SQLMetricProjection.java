/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.request.Argument;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Metric projection that can expand the metric into a SQL projection fragment.
 */
@Value
@Builder
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
        return queryPlanResolver.resolve(this);
    }

    public SQLMetricProjection(Queryable source,
                               String name,
                               ValueType valueType,
                               ColumnType columnType,
                               String expression,
                               String  alias,
                               Map<String, Argument> arguments,
                               QueryPlanResolver queryPlanResolver) {
        this.source = source;
        this.name = name;
        this.valueType = valueType;
        this.columnType = columnType;
        this.expression = expression;
        this.alias = alias;
        this.arguments = arguments;
        this.queryPlanResolver = queryPlanResolver == null ? new DefaultQueryPlanResolver() : queryPlanResolver;
    }

    public SQLMetricProjection(Metric metric,
                               String alias,
                               Map<String, Argument> arguments) {
        this((SQLTable) metric.getTable(), metric.getName(), metric.getValueType(),
                metric.getColumnType(), metric.getExpression(), alias, arguments, metric.getQueryPlanResolver());
    }

    public SQLMetricProjection withSource(Queryable source) {
        return SQLMetricProjection.builder()
                .source(source)
                .name(name)
                .alias(alias)
                .valueType(valueType)
                .columnType(columnType)
                .expression(expression)
                .arguments(arguments)
                .build();
    }
}
