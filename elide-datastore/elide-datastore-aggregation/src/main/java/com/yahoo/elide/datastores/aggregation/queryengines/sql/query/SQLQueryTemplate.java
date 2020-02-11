/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * SQLQueryTemplate contains projections information about a sql query.
 */
public interface SQLQueryTemplate {
    /**
     * Get all invoked metrics in this query.
     *
     * @return invoked metrics
     */
    List<MetricFunctionInvocation> getMetrics();

    /**
     * Get all non-time dimensions in this query.
     *
     * @return non-time dimensions
     */
    Set<ColumnProjection> getNonTimeDimensions();

    /**
     * Get aggregated time dimension for this query.
     *
     * @return time dimension
     */
    TimeDimensionProjection getTimeDimension();

    /**
     * Get all GROUP BY dimensions in this query, include time and non-time dimensions.
     *
     * @return all GROUP BY dimensions
     */
    default Set<ColumnProjection> getGroupByDimensions() {
        return getTimeDimension() == null
                ? getNonTimeDimensions()
                : Sets.union(getNonTimeDimensions(), Collections.singleton(getTimeDimension()));
    }

    /**
     * Get a copy of this query with a requested time grain.
     *
     * @param timeGrain requested time grain
     * @return a copied query template
     */
    default SQLQueryTemplate toTimeGrain(TimeGrain timeGrain) {
        SQLQueryTemplate wrapped = this;
        return new SQLQueryTemplate() {
            @Override
            public List<MetricFunctionInvocation> getMetrics() {
                return wrapped.getMetrics();
            }

            @Override
            public Set<ColumnProjection> getNonTimeDimensions() {
                return wrapped.getNonTimeDimensions();
            }

            @Override
            public TimeDimensionProjection getTimeDimension() {
                return wrapped.getTimeDimension().toTimeGrain(timeGrain);
            }
        };
    }

    /**
     * Merge with other query.
     *
     * @param second other query template
     * @return merged query template
     */
    default SQLQueryTemplate merge(SQLQueryTemplate second) {
        SQLQueryTemplate first = this;
        // TODO: validate dimension
        List<MetricFunctionInvocation> merged = new ArrayList<>(first.getMetrics());
        merged.addAll(second.getMetrics());

        return new SQLQueryTemplate() {
            @Override
            public List<MetricFunctionInvocation> getMetrics() {
                return merged;
            }

            @Override
            public Set<ColumnProjection> getNonTimeDimensions() {
                return first.getNonTimeDimensions();
            }

            @Override
            public TimeDimensionProjection getTimeDimension() {
                return first.getTimeDimension();
            }
        };
    }
}
