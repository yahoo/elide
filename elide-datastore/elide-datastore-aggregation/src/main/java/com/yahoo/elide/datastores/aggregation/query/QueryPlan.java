/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import org.apache.commons.lang3.tuple.Pair;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link QueryPlan} is a partial Query bound to a particular Metric.
 */
@Builder
public class QueryPlan implements Queryable {

    @NonNull
    @Getter
    private Queryable source;

    @Singular
    @NonNull
    @Getter
    private List<MetricProjection> metricProjections;

    @Singular
    @NonNull
    @Getter
    private List<DimensionProjection> dimensionProjections;

    @Singular
    @NonNull
    @Getter
    private List<TimeDimensionProjection> timeDimensionProjections;

    @Getter
    private FilterExpression whereFilter;

    /**
     * Tests whether or not this plan can be nested.
     * @param metaDataStore MetaDataStore.
     * @return True if the projection can be nested.  False otherwise.
     */
    public boolean canNest(MetaDataStore metaDataStore) {
        return getColumnProjections().stream().allMatch(projection -> projection.canNest(source, metaDataStore));
    }

    /**
     * Breaks a flat query into a nested query.  There are multiple approaches for how to do this, but
     * this kind of nesting requires aggregation to happen both in the inner and outer queries.  This allows
     * query plans with two-pass aggregations to be merged with simpler one-pass aggregation plans.
     *
     * The nesting performed here attempts to perform all joins in the inner query.
     *
     * @param metaDataStore MetaDataStore.
     * @return A nested query plan.
     */
    public QueryPlan nest(MetaDataStore metaDataStore) {
        if (!canNest(metaDataStore)) {
            throw new UnsupportedOperationException("Cannot nest this query plan");
        }

        Set<Pair<ColumnProjection, Set<ColumnProjection>>> nestedMetrics = metricProjections.stream()
                .map(projection -> projection.nest(source, metaDataStore, false))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Pair<ColumnProjection, Set<ColumnProjection>>> nestedDimensions = dimensionProjections.stream()
                .map(projection -> projection.nest(source, metaDataStore, false))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Pair<ColumnProjection, Set<ColumnProjection>>> nestedTimeDimensions = timeDimensionProjections.stream()
                .map(projection -> projection.nest(source, metaDataStore, false))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        QueryPlan inner = QueryPlan.builder()
                .source(this.getSource())
                .whereFilter(whereFilter)
                .metricProjections(nestedMetrics.stream()
                        .map(Pair::getRight)
                        .flatMap(Set::stream)
                        .map(MetricProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .dimensionProjections(nestedDimensions.stream()
                        .map(Pair::getRight)
                        .flatMap(Set::stream)
                        .map(DimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .timeDimensionProjections(nestedTimeDimensions.stream()
                        .map(Pair::getRight)
                        .flatMap(Set::stream)
                        .map(TimeDimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();

        return QueryPlan.builder()
                .source(inner)
                .metricProjections(nestedMetrics.stream()
                        .map(Pair::getLeft)
                        .map(MetricProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .dimensionProjections(nestedDimensions.stream()
                        .map(Pair::getLeft)
                        .map(DimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .timeDimensionProjections(nestedTimeDimensions.stream()
                        .map(Pair::getLeft)
                        .map(TimeDimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }
}
