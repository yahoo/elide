/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.request.Sorting;

import com.google.common.collect.Streams;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashSet;
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
    private Set<MetricProjection> metricProjections;

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
    public String getName() {
        return getAlias();
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public ColumnProjection getDimensionProjection(String name) {
        return dimensionProjections.stream()
                .filter(dim -> dim.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public ColumnProjection getColumnProjection(String name) {
        return getColumnProjections().stream()
                .filter(dim -> dim.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public MetricProjection getMetricProjection(String name) {
        return metricProjections.stream()
                .filter(metric -> metric.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public TimeDimensionProjection getTimeDimensionProjection(String name) {
        return timeDimensionProjections.stream()
                .filter(dim -> dim.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<ColumnProjection> getColumnProjections() {
        return Streams.concat(
                timeDimensionProjections.stream(),
                dimensionProjections.stream(),
                metricProjections.stream())
                .collect(Collectors.toSet());
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
