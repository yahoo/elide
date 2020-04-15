/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLMetric;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Argument;

import java.util.Map;

/**
 * Metric projection that can expand the metric into a SQL projection fragment.
 */
public class SQLMetricProjection implements MetricProjection {

    private SQLMetric metric;
    private SQLReferenceTable sqlReferenceTable;
    private String alias;
    private Map<String, Argument> arguments;


    public SQLMetricProjection(SQLMetric metric,
                               SQLReferenceTable sqlReferenceTable,
                               String alias,
                               Map<String, Argument> arguments) {
        this.metric = metric;
        this.sqlReferenceTable = sqlReferenceTable;
        this.arguments = arguments;
        this.alias = alias;
    }

    @Override
    public Metric getColumn() {
        return metric;
    }

    @Override
    public String getFunctionExpression() {
        return sqlReferenceTable.getResolvedReference(metric.getTable(), metric.getName());
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public Map<String, Argument> getArguments() {
        return arguments;
    }
}
