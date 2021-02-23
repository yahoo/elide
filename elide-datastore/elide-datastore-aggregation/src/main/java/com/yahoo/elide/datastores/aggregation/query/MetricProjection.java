/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

/**
 * Represents a projected metric column as an alias in a query.
 */
public interface MetricProjection extends ColumnProjection {

    /**
     * Resolves the query plan that would fetch this particular metric.
     * @return the resolved query plan.
     */
    QueryPlan resolve();
}
