/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan.QueryPlan;

import java.util.Set;

/**
 * SQLMetric would contain {@link SQLMetricFunction} instead of {@link MetricFunction}.
 */
public class SQLMetric extends Metric {
    public SQLMetric(SQLTable table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
    }

    @Override
    protected SQLMetricFunction constructMetricFunction(String id,
                                                     String description,
                                                     String expression,
                                                     Set<FunctionArgument> arguments) {
        return new SQLMetricFunction(id, description, expression, arguments);
    }

    /**
     * Construct a sql query template for a physical table with provided information.
     * Table name would be filled in when convert the template into real query.
     *
     * @param query The entire client query
     * @param metric The metric to resolve.
     * @return <code>SELECT function(arguments, fields) AS alias GROUP BY dimensions, timeDimension </code>
     */
    public QueryPlan resolve(Query query, MetricProjection metric, SQLReferenceTable referenceTable) {
        Query singleMetricQuery = Query.builder()
                .metric(metric)
                .table(query.getTable())
                .groupByDimensions(query.getGroupByDimensions())
                .timeDimensions(query.getTimeDimensions())
                .whereFilter(query.getWhereFilter())
                .havingFilter(query.getHavingFilter())
                .sorting(query.getSorting())
                .pagination(query.getPagination())
                .scope(query.getScope())
                .build();

        return new QueryPlan(singleMetricQuery);
    }
}
