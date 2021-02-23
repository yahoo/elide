/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

/**
 * Default query plan resolver.  Resolves to a simple plan that projects the metric.
 */
public class DefaultQueryPlanResolver implements QueryPlanResolver {
    @Override
    public QueryPlan resolve(MetricProjection projection) {
        return QueryPlan.builder()
                .source(projection.getSource())
                .metricProjection(projection)
                .build();
    }
}
