/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;

/**
 * Represents a projected metric column as an alias in a query.
 */
public interface MetricProjection extends ColumnProjection<Metric> {
    /**
     * Get the projected metric.
     *
     * @return metric column
     */
    @Override
    Metric getColumn();

    /**
     * Returns the metric function associated with the metric.
     * @return the metric function
     */
    MetricFunction getMetricFunction();
}
