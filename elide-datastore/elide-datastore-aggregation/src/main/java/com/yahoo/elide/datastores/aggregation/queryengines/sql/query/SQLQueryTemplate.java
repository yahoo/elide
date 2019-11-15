/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.NotImplementedException;

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
    List<SQLMetricFunctionInvocation> getMetrics();

    /**
     * Get all non-time dimensions in this query
     *
     * @return non-time dimensions
     */
    Set<ColumnProjection> getNonTimeDimensions();

    /**
     * Get aggregated time dimension for this query
     *
     * @return time dimension
     */
    TimeDimensionProjection getTimeDimension();

    /**
     * Indicate whether the query is ran on a physical table directly.
     *
     * @return True if this query is ran on a physical table
     */
    boolean isFromTable();

    /**
     * Get subquery nested in this query.
     *
     * @return null if {@link #isFromTable()}
     */
    SQLQueryTemplate getSubQuery();

    /**
     * Get all GROUP BY dimensions in this query, include time and non-time dimensions
     *
     * @return all GROUP BY dimensions
     */
    default Set<ColumnProjection> getGroupByDimensions() {
        return getTimeDimension() == null
                ? getNonTimeDimensions()
                : Sets.union(getNonTimeDimensions(), Collections.singleton(getTimeDimension()));
    }

    /**
     * Get a query level indicating whether this query is nested. Level of physical table query is 1
     *
     * @return query level
     */
    default int getLevel() {
        return isFromTable() ? 1 : 1 + getSubQuery().getLevel();
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
            public List<SQLMetricFunctionInvocation> getMetrics() {
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

            @Override
            public boolean isFromTable() {
                return wrapped.isFromTable();
            }

            @Override
            public SQLQueryTemplate getSubQuery() {
                return wrapped.getSubQuery();
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
        if (getLevel() != second.getLevel()) {
            // TODO: support different level merging
            throw new InvalidPredicateException("Can't merge two query with different level");
        } else {
            // TODO: support multiple-level merging
            if (getLevel() > 1) {
                throw new NotImplementedException("Merging sql query is not supported.");
            }

            SQLQueryTemplate first = this;
            // TODO: validate dimension
            List<SQLMetricFunctionInvocation> merged = new ArrayList<>(first.getMetrics());
            merged.addAll(second.getMetrics());

            return new SQLQueryTemplate() {
                @Override
                public List<SQLMetricFunctionInvocation> getMetrics() {
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

                @Override
                public boolean isFromTable() {
                    return first.isFromTable();
                }

                @Override
                public SQLQueryTemplate getSubQuery() {
                    return first.getSubQuery();
                }
            };
        }
    }
}
