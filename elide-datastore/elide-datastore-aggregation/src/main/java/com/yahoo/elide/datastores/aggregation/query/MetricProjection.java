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
     * Resolves the query that would fetch this particular metric.
     * @return the resolved query.
     */
    default Query resolve() {
        return Query.builder()
                .metricProjection(this)
                .source(getSource())
                .build();
    }
}
