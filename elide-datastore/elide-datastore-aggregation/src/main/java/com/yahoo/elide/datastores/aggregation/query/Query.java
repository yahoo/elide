/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.request.Sorting;

import com.google.common.collect.Streams;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Query} is an object representing a query executed by {@link QueryEngine}.
 */
@Value
@Builder
public class Query implements Queryable {
    @NonNull
    private Queryable source;

    @Singular
    @NonNull
    private List<MetricProjection> metricProjections;

    @Singular
    @NonNull
    private Set<ColumnProjection> dimensionProjections;

    @Singular
    @NonNull
    private Set<TimeDimensionProjection> timeDimensionProjections;

    private FilterExpression whereFilter;
    private FilterExpression havingFilter;
    private Sorting sorting;
    private ImmutablePagination pagination;
    private RequestScope scope;

    /**
     * Whether to bypass the {@link QueryEngine} cache for this query.
     */
    private boolean bypassingCache;

    /**
     * Returns all the dimensions regardless of type.
     * @return All the dimensions.
     */
    public Set<ColumnProjection> getAllDimensionProjections() {
        return Stream.concat(getDimensionProjections().stream(), getTimeDimensionProjections().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getAlias() {
        return source.getAlias() + "_" + this.hashCode();
    }

    @Override
    public Dimension getDimension(String name) {
        return dimensionProjections.stream()
                .filter(dim -> dim.getColumn().getName().equals(name))
                .map(ColumnProjection::getColumn)
                .map(Dimension.class::cast)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<Dimension> getDimensions() {
        return dimensionProjections.stream()
                .map(ColumnProjection::getColumn)
                .map(Dimension.class::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public Metric getMetric(String name) {
        return metricProjections.stream()
                .filter(metric -> metric.getColumn().getName().equals(name))
                .map(ColumnProjection::getColumn)
                .map(Metric.class::cast)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<Metric> getMetrics() {
        return metricProjections.stream()
                .map(ColumnProjection::getColumn)
                .map(Metric.class::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public TimeDimension getTimeDimension(String name) {
        return timeDimensionProjections.stream()
                .filter(dim -> dim.getColumn().getName().equals(name))
                .map(ColumnProjection::getColumn)
                .map(TimeDimension.class::cast)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<TimeDimension> getTimeDimensions() {
        return timeDimensionProjections.stream()
                .map(ColumnProjection::getColumn)
                .map(TimeDimension.class::cast)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Column> getColumns() {
        return Streams.concat(
                timeDimensionProjections.stream().map(ColumnProjection::getColumn),
                dimensionProjections.stream().map(ColumnProjection::getColumn),
                metricProjections.stream().map(ColumnProjection::getColumn))
                .collect(Collectors.toSet());
    }

    public Set<ColumnProjection> getColumnProjections() {
        return Streams.concat(timeDimensionProjections.stream(),
                dimensionProjections.stream(),
                metricProjections.stream()).collect(Collectors.toSet());
    }

    @Override
    public String getDbConnectionName() {
        return source.getDbConnectionName();
    }

    @Override
    public <T> T accept(QueryVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }
}
