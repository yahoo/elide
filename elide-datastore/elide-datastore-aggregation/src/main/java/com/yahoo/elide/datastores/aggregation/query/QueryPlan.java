/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.google.common.collect.Streams;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link QueryPlan} is a partial Query bound to a particular Metric.
 */
@Value
@Builder
public class QueryPlan implements Queryable {

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

    /**
     * Merges two query plans together.  The order of merged metrics and dimensions is preserved such that
     * the current plan metrics and dimensions come after the requested plan metrics and dimensions.
     * @param other The other query to merge.
     * @return A new merged query plan.
     */
    public QueryPlan merge(QueryPlan other) {
        //TODO - Add support for nesting and more complex merges.

        if (other == null) {
            return this;
        }

        assert getSource().equals(other.getSource());

        List<MetricProjection> metrics = Streams.concat(other.metricProjections.stream(), metricProjections.stream())
                .collect(Collectors.toList());

        List<TimeDimensionProjection> timeDimensions = Streams.concat(other.timeDimensionProjections.stream(),
                timeDimensionProjections.stream()).collect(Collectors.toList());

        List<ColumnProjection> dimensions = Streams.concat(other.dimensionProjections.stream(),
                dimensionProjections.stream()).collect(Collectors.toList());

        return QueryPlan.builder()
                .source(getSource())
                .metricProjections(metrics)
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .build();
    }
}
