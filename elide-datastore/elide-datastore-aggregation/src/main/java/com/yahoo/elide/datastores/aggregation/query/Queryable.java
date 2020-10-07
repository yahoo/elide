/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import java.util.Set;

/**
 * Represents something that can be queried.  There are generally, three kinds:
 * - Tables
 * - Queries (a subquery)
 * - Joins (a join between multiple tables or subqueries).
 */
public interface Queryable {

    /**
     * Every queryable needs an alias which uniquely identifies the queryable in an individual query
     * @return The alias
     */
    public String getAlias();

    /**
     * Looks up the alias for a particular column.
     * @param columnName The name of the column.
     * @return The alias for the given column.
     */
    default public String getAlias(String columnName) {
        return getAlias();
    }

    /**
     * Retrieves a non-time dimension by name.
     * @param name The name of the dimension.
     * @return The dimension.
     */
    public Dimension getDimension(String name);

    /**
     * Retrieves all the non-time dimensions.
     * @return The non-time dimensions.
     */
    public Set<Dimension> getDimensions();

    /**
     * Retrieves a metric by name.
     * @param name The name of the metric.
     * @return The metric.
     */
    public Metric getMetric(String name);

    /**
     * Retrieves all the metrics.
     * @return The metrics.
     */
    public Set<Metric> getMetrics();

    /**
     * Retrieves a time dimension by name.
     * @param name The name of the time dimension.
     * @return The time dimension.
     */
    public TimeDimension getTimeDimension(String name);

    /**
     * Retrieves all the time dimensions.
     * @return The time dimensions.
     */
    public Set<TimeDimension> getTimeDimensions();

    /**
     * Returns all the columns.
     * @return the columns.
     */
    public Set<Column> getColumns();

    /**
     * Returns the connection name where this queryable is sourced from.
     * @return the connectinon name
     */
    public String getDbConnectionName();

    /**
     * Execute a visitor on this query.
     * @param visitor The visitor to execute.
     * @param <T> The return type of the visitor.
     * @return Something that the visitor is constructing.
     */
    public <T> T accept(QueryVisitor<T> visitor);
}
