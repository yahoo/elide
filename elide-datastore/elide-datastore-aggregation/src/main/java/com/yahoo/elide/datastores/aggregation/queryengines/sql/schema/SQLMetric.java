/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.schema;

import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.metric.AggregatedMetric;
import com.yahoo.elide.datastores.aggregation.schema.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.schema.metric.Metric;

import java.util.List;

/**
 * A metric with physical column name needed to generate SQL.
 */
public class SQLMetric extends AggregatedMetric {
    private final String columnName;

    /**
     * Constructor.
     *
     * @param schema       The schema this {@link Metric} belongs to
     * @param metricField  The entity field or relation that this {@link Metric} represents
     * @param annotation   Provides static meta data about this {@link Metric}
     * @param fieldType    The Java type for this entity field or relation
     * @param aggregations A list of all supported aggregations on this {@link Metric}
     * @param columnName   Physical column name of this metric in SQL database.
     */
    public SQLMetric(
            Schema schema,
            String metricField,
            Meta annotation,
            Class<?> fieldType,
            List<Class<? extends Aggregation>> aggregations,
            String columnName
    ) {
        super(schema, metricField, annotation, fieldType, aggregations);
        this.columnName = columnName;
    }

    @Override
    protected String getMetricName() {
        return schema.getAlias() + "." + columnName;
    }
}
