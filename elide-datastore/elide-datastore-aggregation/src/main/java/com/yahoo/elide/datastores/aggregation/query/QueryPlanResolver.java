/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

/**
 * Resolves a projected metric into a query plan.
 */
@FunctionalInterface
public interface QueryPlanResolver {

    /**
     * Resolves a projected metric into a query plan.
     * @param source The queryable that contains the metric.
     * @param projection The metric being projected.
     * @return A query plan for the particular metric.
     */
    public QueryPlan resolve(Query source, MetricProjection projection);
}
